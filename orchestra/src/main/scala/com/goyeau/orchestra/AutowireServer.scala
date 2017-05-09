package com.goyeau.orchestra

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._

object AutowireServer extends autowire.Server[String, Decoder, Encoder] {
  override def read[T: Decoder](json: String): T =
    decode[T](json).fold(throw _, identity)

  override def write[T: Encoder](obj: T): String = obj.asJson.noSpaces
}