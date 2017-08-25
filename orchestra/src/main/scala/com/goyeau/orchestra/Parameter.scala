package com.goyeau.orchestra

import java.util.UUID

import enumeratum._

sealed trait Parameter[T] {
  lazy val id: Symbol = Symbol(name.toLowerCase.replaceAll("\\s", ""))
  def name: String
  def defaultValue: Option[T]
  def getValue(valueMap: Map[Symbol, Any]): T =
    valueMap
      .get(id)
      .map(_.asInstanceOf[T])
      .orElse(defaultValue)
      .getOrElse(throw new IllegalArgumentException(s"Can't get param ${id.name}"))
}

case class Param[T](name: String, defaultValue: Option[T] = None) extends Parameter[T]

case class EnumParam[Entry <: EnumEntry](name: String, enum: Enum[Entry], defaultValue: Option[Entry] = None)
    extends Parameter[Entry]

object RunId extends Parameter[UUID] {
  val name = "Run ID"
  def defaultValue = None
}
