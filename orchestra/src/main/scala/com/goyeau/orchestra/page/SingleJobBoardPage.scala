package com.goyeau.orchestra.page

import java.util.UUID

import scala.concurrent.ExecutionContext

import autowire._
import com.goyeau.orchestra._
import com.goyeau.orchestra.ARunStatus._
import com.goyeau.orchestra.parameter.Parameter.State
import com.goyeau.orchestra.parameter.{ParameterOperations, RunId}
import com.goyeau.orchestra.route.WebRouter.{AppPage, TaskLogsPage}
import io.circe._
import io.circe.java8.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

object SingleJobBoardPage {

  def component[Params <: HList, ParamValues <: HList: Encoder, Result: Decoder](
    name: String,
    job: Job.Definition[_, ParamValues, Result],
    params: Params,
    ctrl: RouterCtl[AppPage]
  )(implicit ec: ExecutionContext, paramOperations: ParameterOperations[Params, ParamValues]) =
    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .initialState {
        val jobInfo = RunInfo(job.id, Option(UUID.randomUUID()))
        (jobInfo, Map[Symbol, Any](RunId.id -> jobInfo.runId), <.div("Loading runs"))
      }
      .render { $ =>
        def runJob(event: ReactEventFromInput) = Callback.future {
          event.preventDefault()
          job.Api.client.run($.state._1, paramOperations.values(params, $.state._2)).call().map {
            case ARunStatus.Failure(e) => Callback.alert(e.getMessage)
            case _                     => ctrl.set(TaskLogsPage(job, $.state._1.runId))
          }
        }

        val displayState = State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))

        <.div(
          <.div(name),
          <.form(^.onSubmit ==> runJob)(
            paramOperations.displays(params, displayState) :+
              <.button(^.`type` := "submit")("Run"): _*
          ),
          <.div("History"),
          $.state._3
        )
      }
      .componentDidMount { $ =>
        Callback.future(
          job.Api.client
            .runs()
            .call()
            .map { runs =>
              val runDisplays = runs.map {
                case (uuid, createdAt) =>
                  <.div(<.button(^.onClick --> ctrl.set(TaskLogsPage(job, uuid)), s"${uuid.toString} - $createdAt"))
              }
              $.modState(_.copy(_3 = if (runDisplays.nonEmpty) <.div(runDisplays: _*) else <.div("No job ran yet")))
            }
        )
      }
      .build
      .apply()
}
