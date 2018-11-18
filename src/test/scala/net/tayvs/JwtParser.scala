package net.tayvs

import akka.actor.ActorSystem
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import net.tayvs.JwtParser.ApplicationException
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import JwtParser.ErrorParseJwt
import pdi.jwt.{Jwt, JwtOptions}

import cats.syntax.either._
import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.util.Try

trait JwtParser extends ErrorAccumulatingCirceSupport {

  implicit def system: ActorSystem

  private implicit lazy val ec: ExecutionContext = system.dispatcher

  def jwtParser: JwtParser

  class JwtParser {

    import JwtParser._

    def claim(token: String): Option[JwtClaim] = {
      val jwtOpts = JwtOptions(signature = false, expiration = false, notBefore = false)

      Jwt
        .decode(token, jwtOpts)
        .flatMap(claimJson => decode[JwtClaim](claimJson).toTry)
        .toOption
    }

    def token(httpHeader: HttpHeader): Option[String] = {
      httpHeader match {
        case Authorization(credentials: OAuth2BearerToken) => Option(credentials.token)
        case _                                             => None
      }
    }

  }

  def brandId(token: String): String = {
    jwtParser.claim(token).map(_.brandId).getOrElse(throw ApplicationException(ErrorParseJwt))
  }

  def userId(token: String): String = {
    jwtParser.claim(token).map(_.user_uuid).getOrElse(throw ApplicationException(ErrorParseJwt))
  }

  def department(token: String): String = {
    jwtParser.claim(token).map(_.department).getOrElse(throw ApplicationException(ErrorParseJwt))
  }

}

object JwtParser {

  import io.circe._, io.circe.generic.semiauto._

  final val ErrorParseJwt = "error.jwt.parse"

  case class ApplicationException(error: String) extends Exception(error)

  implicit val jwtClaimDecoder: Decoder[JwtClaim] = deriveDecoder
  implicit val jwtClaimEncoder: Encoder[JwtClaim] = deriveEncoder

  case class JwtClaim(brandId: String, user_uuid: String, department: String)

}
