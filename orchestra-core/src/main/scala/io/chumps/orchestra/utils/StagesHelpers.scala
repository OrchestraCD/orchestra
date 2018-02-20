package io.chumps.orchestra.utils

import java.io.IOException
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.DynamicVariable

import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import shapeless._

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.model.Indexed._
import io.chumps.orchestra.utils.AkkaImplicits._

trait StagesHelpers {

  def stage[Result](name: String)(f: => Result): Result = Await.result(
    for {
      run <- Elasticsearch.client
        .execute(get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(OrchestraConfig.runInfo)))
        .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Run[HNil, Unit]])

      stageStart = Stage(OrchestraConfig.runInfo, run.parentJob, name, Instant.now(), Instant.now())
      stageIndexResponse <- Elasticsearch.client
        .execute(indexInto(StagesIndex.index, StagesIndex.`type`).source(stageStart))
        .map(_.fold(failure => throw new IOException(failure.error.reason), identity))

      runningPong = system.scheduler.schedule(1.second, 1.second) {
        Elasticsearch.client
          .execute(
            updateById(StagesIndex.index.name, StagesIndex.`type`, stageIndexResponse.result.id)
              .source(stageStart.copy(latestUpdateOn = Instant.now()))
          )
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
      }
    } yield
      try StagesHelpers.stageVar.withValue(Option(name)) {
        println(s"Stage: $name")
        f
      } finally runningPong.cancel(),
    Duration.Inf
  )
}

object StagesHelpers {
  private[orchestra] val stageVar = new DynamicVariable[Option[String]](None)
}
