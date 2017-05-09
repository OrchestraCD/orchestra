package com.goyeau.orchestra

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax

case class AutowireClient(taskId: Symbol) extends autowire.Client[String, Decoder, Encoder] {
  override def doCall(req: Request): Future[String] =
    Ajax
      .post(
        url = ("api" +: taskId.name +: req.path).mkString("/"),
        data = req.args.asJson.noSpaces,
        responseType = "application/json",
        headers = Map("Content-Type" -> "application/json")
      )
      .map(_.responseText)

  override def read[T: Decoder](json: String) =
    decode[T](json) match {
      case Left(e) => throw new Exception(e)
      case Right(result) => result
    }

  override def write[T: Encoder](r: T) = r.asJson.noSpaces
}