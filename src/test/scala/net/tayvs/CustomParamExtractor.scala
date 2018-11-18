package net.tayvs

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.headerValueByName
import akka.http.scaladsl.server.directives.ParameterDirectives.{ParamDef, ParamDefAux, ParamMagnet}
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller

trait CustomParamExtractor {
  this: Test =>

  sealed abstract class HeaderExtractor(val name: String)

  case object NameHeader extends HeaderExtractor("name")

  case object AgeHeader extends HeaderExtractor("age")

  implicit def nameHeaderEx[T <: HeaderExtractor](implicit fsu: FromStringUnmarshaller[String]): ParamDefAux[T, Directive1[String]] = {
    ParamDef.paramDef[T, Directive1[String]] { nameEX => headerValueByName(nameEX.name) }
  }

  implicit def nameHeaderParamMagnet(nameHeader: HeaderExtractor): ParamMagnet {type Out = Directive1[String]} =
    ParamMagnet[HeaderExtractor](nameHeader)

  val authHeader: Directive1[String] = headerValueByName("Authentication")

}
