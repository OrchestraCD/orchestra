package io.chumps.orchestra.job

import scala.language.{higherKinds, implicitConversions}

import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless._

import io.chumps.orchestra.board.Job
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.page.JobPage
import io.chumps.orchestra.parameter.ParameterOperations
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class SimpleJob[Func, ParamValues <: HList: Encoder: Decoder, Params <: HList](
  id: Symbol,
  name: String,
  params: Params
)(implicit paramOperations: ParameterOperations[Params, ParamValues])
    extends Job[Func, ParamValues] {

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    val breadcrumb = parentBreadcrumb :+ id.name.toLowerCase
    dynamicRoute(
      id.name.toLowerCase ~ ("/" ~ uuid).option
        .xmap(uuid => BoardPageRoute(breadcrumb, uuid.map(RunId(_))))(_.runId.map(_.value))
    ) {
      case p @ BoardPageRoute(bc, _) if bc == breadcrumb => p
    } ~> dynRenderR((page, ctrl) => JobPage.component(JobPage.Props(this, params, page.runId, ctrl)))
  }
}
