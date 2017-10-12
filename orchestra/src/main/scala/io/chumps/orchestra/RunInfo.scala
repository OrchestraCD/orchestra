package io.chumps.orchestra

import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import shapeless._

case class RunInfo(job: Job.Definition[_, _ <: HList, _], runId: UUID)

object RunInfo {

  implicit val encoder: Encoder[RunInfo] = (o: RunInfo) =>
    Json.obj(
      "job" -> o.job.asJson,
      "runId" -> o.runId.asJson
  )

  implicit val decoder: Decoder[RunInfo] = (c: HCursor) =>
    for {
      job <- c.downField("job").as[Job.Definition[_, _ <: HList, _]]
      runId <- c.downField("runId").as[UUID]
    } yield RunInfo(job, runId)

  def decodeWithFallbackRunId(runInfoJson: String, fallBackRunId: UUID) =
    decode[RunInfo](runInfoJson).fold(
      _ => RunInfo(decode[Job.Definition[_, _ <: HList, _]](runInfoJson).fold(throw _, identity), fallBackRunId),
      identity
    )
}
