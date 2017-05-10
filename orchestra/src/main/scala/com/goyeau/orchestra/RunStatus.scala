package com.goyeau.orchestra

import java.time.Instant
import java.util.UUID

import io.circe._
import io.circe.generic.semiauto._

sealed trait RunStatus
object RunStatus {
  case class Running(since: Long) extends RunStatus
  case object Success extends RunStatus
//  case class Running[T](since: Instant) extends RunStatus[T]
////  case class Success[Result: Encoder](r: Result) extends RunStatus
  case class Failed(e: Throwable) extends RunStatus

  implicit val encodeFailed = new Encoder[Failed] {
    final def apply(o: Failed): Json = Json.obj(
      (
        "e",
        Json.obj(
          ("message", Json.fromString(o.e.getMessage))
        )
      )
    )
  }

  implicit val decodeFailed = new Decoder[Failed] {
    final def apply(c: HCursor): Decoder.Result[Failed] =
      for {
        message <- c.downField("e").downField("message").as[String]
      } yield Failed(new Throwable(message))
  }
}

case class RunInfo(id: UUID)
