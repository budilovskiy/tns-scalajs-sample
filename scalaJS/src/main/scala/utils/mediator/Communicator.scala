/**
  * Copyright 2017, Alexander Ray (dev@alexray.me)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  */

package utils.mediator

import shared.Common._
import shared.utils.{DecodeError, TransportDecoder, TransportEncoder, TransportSerializer}
import utils.mediator.strategies.ReconnectStrategy
import utils.mediator.strategies.ReconnectStrategy.Transports
import utils.mediator.transport.TransportFactory

import scala.collection.immutable.Queue
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.timers.setTimeout

trait Communicator {
  import Communicator._

  def addTransport(tf: TransportFactory): Unit

  def send[T](message: T)(implicit encoder: TransportEncoder[T, String]): Unit
  def onRawMessage(f: MessageType => Unit): Unit
  def onMessage[T](f: Either[DecodeError, T] => Unit)(implicit decoder: TransportDecoder[T, String]): Unit
  def onDecodedMessage[T](f: T => Unit)(implicit decoder: TransportDecoder[T, String]): Unit
  def ?[Rq, Rs](q: Rq)(implicit as: TransportSerializer[Rq, Rs, String]): Future[Rs]
}

trait CommunicatorFactory {

  def createCommunicator(): Communicator

}

object CommunicatorFactory {}

object Communicator {

  type MessageType = String
  type MessageEvent = MessageType => Unit

  def apply(tf: TransportFactory, fallback: TransportFactory*)
           (implicit reconnectStrategy: ReconnectStrategy,
            ts: TransportSerializer[String, String, String])
  : Communicator =
  {
    val res: CommunicatorOps = new CommunicatorOps(reconnectStrategy, ts)
    res.addTransport(tf)
    fallback.foreach(res.addTransport)
    res.connect()
    res
  }
}

class CommunicatorOps(reconnectStrategy: ReconnectStrategy,
                      ts: TransportSerializer[String, String, String])
  extends Communicator
{

  import Communicator._

  private val reconnectionTimeout = 10000 // ms

  type ResponseProcessor = ResponseMessage => Unit

  private var requests = Map.empty[UID, ResponseProcessor]
  private var stashedMessages: Queue[MessageType] = Queue()
  private var transports: Transports = Queue.empty[TransportFactory]
  private var messageEventListeners = List.empty[MessageEvent]
  private var connected = false
  private var internalSend: MessageEvent = stash

  private def reconnect(): Unit = {
    setTimeout(reconnectionTimeout) { connect() }
  }

  def connect(): Unit = {
    transports = reconnectStrategy(transports) { factory =>
      val t = factory.createTransport()

      t.onConnect { _ =>
        connected = true
        internalSend = (message : MessageType) => t.send(message)
        unstashAll()
      }

      t.onMessage { m =>
//        info(s"t.onMessage { m => count: ${messageEventListeners.size}")
        messageEventListeners.foreach(l => l(m))
      }

      t.onDisconnect { _ =>
        connected = false
        reconnect()
      }

      t.connect()
    }

  }

  private def stash(message: String): Unit = stashedMessages = stashedMessages.enqueue(message)
  private def unstashAll(): Unit = {
    val q = stashedMessages
    stashedMessages = Queue()
    q.foreach(internalSend)
  }

  def addTransport(t: TransportFactory): Unit = transports +:= t

  def send[T](message: T)(implicit encoder: TransportEncoder[T, String]): Unit = {
    internalSend(encoder.encode(message))
  }

  def onMessage[T](f: Either[DecodeError, T] => Unit)(implicit decoder: TransportDecoder[T, String]): Unit = {
//    info("added onMessage[T](f: Either[String, T] => Unit)(implicit decoder: TransportDecoder[T]): Unit")
    val listener: MessageEvent = (message: MessageType) =>  {
//      info(s"val listener: MessageEvent = (message: MessageType) =>  { '$message' ")
      val s: Either[DecodeError, T] = decoder.decode(message)
//      info(s"decoded: '$s'")
      f(s)
    }

    messageEventListeners +:= listener
  }

  def onDecodedMessage[T](f: T => Unit)(implicit decoder: TransportDecoder[T, String]): Unit =
    onMessage { x: Either[DecodeError, T] => x match {
      case Right(m) =>
//        info(s"case Right(m) => '$m'")
        f(m)
      case Left(e) => //error(s"onDecodedMessage: wrong type or decoding error '$e' - ")
    }}

  override def onRawMessage(f: MessageEvent): Unit = messageEventListeners +:= f

  onDecodedMessage[String]{
//    case m: ResponseMessage =>
//      requests.get(m.uid).foreach(f => f(m))
//      requests -= m.uid
    case msg => ()
  }(ts.decoder)

  def ?[Rq, Rs](q: Rq)(implicit as: TransportSerializer[Rq, Rs, String]): Future[Rs] = ask(q)(as)

  def ask[Request, Response](q: Request)(as: TransportSerializer[Request, Response, String]):
  Future[Response] =
  {
//    info(s"def ask[Request, Response](q: Request) - '$q'")
    val uid = UID()
    val request: String  = q.toString

    val p = Promise[Response]()

    val f: ResponseProcessor = (response: ResponseMessage) => {
//      info(s"ResponseProcessor - $response")
//      info(s"decoder - $as")
      response.transportMessage match {
//        case msg: PlainMessage =>
//          as.decode(msg) match {
//            case Left(e: DecodeError) =>
//              //error(s"decoding failed: $e")
//              p.failure(new Throwable(e.toString))
//            case Right(m) =>
////              info(s"decoded: $m")
//              p.success(m)
//          }
        case msg =>
          val e = s"unknown transport message type. required PlainMessage received: '$msg'"
//          error(e)
          p.failure(new Throwable(e))
      }
    }

    requests += uid -> f

    val x = ts.encode(request)
//    info(s"request $x")

    send(request)(ts.encoder)

    p.future
  }
}
