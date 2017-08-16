/**
  * Copyright 2017, Alexander Ray (dev@alexray.me)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  */

package utils.mediator.transport

import shared.entities.FacadeTypes.SocketIO

case class SocketIOConfig(socket: SocketIO) {}

trait SocketIOTransportFactory extends TransportFactory {

  def config: SocketIOConfig

  override def createTransport(): Transport = SocketIOTransport(config)

}

object SocketIOTransportFactory {
  def apply(c: SocketIOConfig): SocketIOTransportFactory = new SocketIOTransportFactory {
    override def config: SocketIOConfig = c
  }
}

trait SocketIOTransport extends Transport {

  type Config = SocketIOConfig
  type TransportEvent = String
  type TransportError = String
  type TransportReceiveMessage = String
  type TransportSendMessage = String

  private var socket: Option[SocketIO] = None
  val timeout = 10000 // ms
  val tryReconnect = false

  def internalSend(data: String): Unit = {
    socket.foreach(_.emit("send message", data))
  }

  def internalConnect(config: Config): Unit = {
    socket = Some(config.socket)
    socket.foreach(socket => {
      socket.on("connect", () => internalOnConnect("Connected"))
      socket.on("disconnect", () => internalOnDisconnect("Disconnected"))
      socket.on("error", (error: String) => internalOnError(s"ERROR: $error"))
      socket.on("message received", (message: String) => internalOnMessage(message))
      socket.connect()
    })
  }

  def encode(value: String): String = value
  def decode(message: TransportReceiveMessage): String = message

  onConnect(e => ())
  onDisconnect(e => ())
  onError(e => ())
  onRawMessage(m => ())
  onMessage(m => ())
  onSendMessage(m => ())

}

object SocketIOTransport {
  def apply(config: SocketIOConfig): Transport = {
    val t = new SocketIOTransport(){}
    t.setConfig(config)
    t
  }

  //  implicit object eventEncoder extends TransportEncoder[Event, String] {
  //    override def transform(value: Event): String = JSON.stringify(value)
  //  }
}