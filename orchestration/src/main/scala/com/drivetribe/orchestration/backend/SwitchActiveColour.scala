package com.drivetribe.orchestration.backend

import com.drivetribe.orchestration._
import com.drivetribe.orchestration.infrastructure.Colour.getActive
import com.drivetribe.orchestration.infrastructure._
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger

object SwitchActiveColour {

  def jobDefinition(environment: Environment) = Job[() => Unit](Symbol(s"switchActiveColour$environment"))

  def job(environment: Environment) = jobDefinition(environment)(apply(environment) _)

  def board(environment: Environment) = SingleJobBoard("Switch Active Colour", jobDefinition(environment))

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(): Unit = {
    val activeColour = getActive(environment)
    val inactiveColour = activeColour.opposite
    logger.info(s"Switching from * $activeColour * to * $inactiveColour *")

    val tfState = TerraformState.fromS3(environment)
    val activeLoadBalancer = tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.active", "arn")
    val inactiveLoadBalancer = tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.inactive", "arn")

    val monitoringActiveLoadBalancer =
      tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.monitoring_active", "arn")
    val monitoringInactiveLoadBalancer =
      tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.monitoring_inactive", "arn")

    val activeAutoScaling =
      tfState.getResourceAttribute(Seq("root", s"rest_api_$activeColour"), "aws_autoscaling_group.api", "name")
    val inactiveAutoScaling =
      tfState.getResourceAttribute(Seq("root", s"rest_api_$inactiveColour"), "aws_autoscaling_group.api", "name")

    logger.info("Scale up inactive")
    AutoScaling.setDesiredCapacity(inactiveAutoScaling, AutoScaling.getDesiredCapacity(activeAutoScaling))

    logger.info("Attach inactive")
    AutoScaling.detachTargetGroups(inactiveAutoScaling, Seq(inactiveLoadBalancer, monitoringInactiveLoadBalancer))
    AutoScaling.attachTargetGroups(inactiveAutoScaling, Seq(activeLoadBalancer, monitoringActiveLoadBalancer))

    logger.info("Wait")
    while (!Colour.isHealthy(activeLoadBalancer)) Thread.sleep(5000)
    while (!AutoScaling.isDesiredCapacity(inactiveAutoScaling)) Thread.sleep(5000)

    logger.info("Detach active")
    AutoScaling.detachTargetGroups(activeAutoScaling, Seq(activeLoadBalancer, monitoringActiveLoadBalancer))
    AutoScaling.attachTargetGroups(activeAutoScaling, Seq(inactiveLoadBalancer, monitoringInactiveLoadBalancer))

    logger.info(s"Switched from * $activeColour * to * $inactiveColour *")
  }
}
