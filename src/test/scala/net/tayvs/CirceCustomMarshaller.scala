package net.tayvs

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

import scala.collection.immutable.Seq

trait CirceCustomMarshaller {
  this: Test =>

  case class Foo(value: String)

  case class Bar(name: String)

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
  implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  implicit val barDecoder: Decoder[Bar] = deriveDecoder[Bar]
  implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]

  implicit val jsonFromHeaderUnmarshaller: FromRequestUnmarshaller[Json] =
    Unmarshaller.apply(_ => {
      case HttpRequest(_, _, headers, entity, _) if entity.contentType == ContentTypes.NoContentType
        || unmarshallerContentTypes.exists(_ matches entity.contentType) =>
        val headerJson = headers
          .find(header => header.name() equals "Auth")
          .map(el => ByteString(el.value()).asByteBuffer)
          .map(jawn.parseByteBuffer(_).fold(throw _, identity))
          .getOrElse(Json.fromJsonObject(JsonObject.empty))

        val bodyByteStrJson = entity match {
          case HttpEntity.Strict(_, data) => FastFuture.successful(data)
          case ent => ent.dataBytes.runFold(ByteString.empty)(_ ++ _)
        }
        bodyByteStrJson
          .map(bs => Some(bs)
            .filter(_.nonEmpty)
            .map(bs => jawn.parseByteBuffer(bs.asByteBuffer).fold(throw _, identity))
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