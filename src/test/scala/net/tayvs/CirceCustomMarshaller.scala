package net.tayvs

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import cats.syntax.either._
import io.circe._, io.circe.parser._


import scala.collection.immutable.Seq

trait CirceCustomMarshaller {
  this: Test  with JwtParser =>

  case class Foo(brandId: String)

  case class Bar(department: String)

  case class CreateRequest(brandId: String, content: String)

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
  implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  implicit val barDecoder: Decoder[Bar] = deriveDecoder[Bar]
  implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
  implicit val createReqDecoder: Decoder[CreateRequest] = deriveDecoder
  implicit val createReqEncoder: Encoder[CreateRequest] = deriveEncoder

  implicit val jsonFromHeaderUnmarshaller: FromRequestUnmarshaller[Json] =
    Unmarshaller.apply(_ => {
      case HttpRequest(_, _, headers, entity, _) if entity.contentType == ContentTypes.NoContentType
        || unmarshallerContentTypes.exists(_ matches entity.contentType) =>
        val headerJson = headers
          .collectFirst {
            case Authorization(credentials: OAuth2BearerToken) => credentials.token
          }
          .flatMap(str => jwtParser.decodeJwt(str).flatMap(json => parse(json).toOption))
          .getOrElse(Json.fromJsonObject(JsonObject.empty))

        val bodyByteStrJson = entity match {
          case HttpEntity.Strict(_, data) => FastFuture.successful(data)
          case ent => ent.dataBytes.runFold(ByteString.empty)(_ ++ _)
        }
        bodyByteStrJson
          .map(bs => Some(bs)
            .filter(_.nonEmpty)
            .flatMap(bs => parse(bs.utf8String).toOption)
            .getOrElse(Json.fromJsonObject(JsonObject.empty)))
          .map(bodyJson => bodyJson deepMerge headerJson)
    })

  implicit def unmarshallerFromHeader[A: Decoder]: FromRequestUnmarshaller[A] = {
    def decode(json: Json) =
      Decoder[A]
        .accumulating(json.hcursor)
        .fold(failures => throw ErrorAccumulatingCirceSupport.DecodingFailures(failures), identity)

    jsonFromHeaderUnmarshaller.map(decode)
  }

}