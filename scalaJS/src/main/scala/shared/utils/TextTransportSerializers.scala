/**
  * Copyright 2017, Alexander Ray (dev@alexray.me)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  */

package shared.utils

object TextTransportSerializers {

  implicit def ts(implicit te: TransportEncoder[String, String], td: TransportDecoder[String, String]) = new TransportSerializer[String, String, String] {
    override def encode(value: String): String = encoder.encode(value)
    override def decode(value: String): Either[DecodeError, String] = decoder.decode(value)
    override def encoder: TransportEncoder[String, String] = te
    override def decoder: TransportDecoder[String, String] = td
  }

  implicit object stringEncoder extends TransportEncoder[String, String] {
    override def transform(value: String): String = value
  }
  implicit object stringDecoder extends TransportDecoder[String, String] {
    override def transform(value: String): Either[DecodeError, String] = Right(value)
  }

}