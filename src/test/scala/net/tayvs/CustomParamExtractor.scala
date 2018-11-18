package net.tayvs

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ParameterDirectives.{ParamDef, ParamDefAux, ParamMagnet}
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import net.tayvs.JwtParser.JwtClaim

trait CustomParamExtractor {
  this: Test with JwtParser =>

  sealed abstract class JwtExtractor {
    def getValue(jwtClaim: JwtClaim): String
  }

  case object BrandId extends JwtExtractor {
    override def getValue(jwtClaim: JwtClaim): String = jwtClaim.brandId
  }

  case object UserUUID extends JwtExtractor {
    override def getValue(jwtClaim: JwtClaim): String = jwtClaim.user_uuid
  }

  case class CustomExtractor(f : JwtClaim => String) extends JwtExtractor {
    override def getValue(jwtClaim: JwtClaim): String = f(jwtClaim)
  }

  implicit def nameHeaderEx[T <: JwtExtractor](implicit fsu: FromStringUnmarshaller[String]): ParamDefAux[T, Directive1[String]] = {
    ParamDef.paramDef[T, Directive1[String]] { nameEX =>
      headerValue(jwtParser.token(_).flatMap(jwtParser.claim)).map(jwtClaim => nameEX.getValue(jwtClaim))
    }
  }

  implicit def nameHeaderParamMagnet(nameHeader: JwtExtractor): ParamMagnet {type Out = Directive1[String]} =
    ParamMagnet[JwtExtractor](nameHeader)

}
