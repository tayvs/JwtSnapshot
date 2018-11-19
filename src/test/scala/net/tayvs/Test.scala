package net.tayvs

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ParameterDirectives.{ParamDef, ParamDefAux, ParamMagnet}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import de.heikoseeberger.akkahttpcirce.{BaseCirceSupport, ErrorAccumulatingCirceSupport}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.breakOut

class Test extends FunSuite with Matchers with ScalatestRouteTest
  with CirceCustomMarshaller with CustomParamExtractor
  with ErrorAccumulatingCirceSupport with JwtParser {

  override def jwtParser: JwtParser = new JwtParser

  val route: Route = get {
    pathEndOrSingleSlash {
      parameter(BrandId) { (name) =>
        complete(s"$name")
      }
    }
  } ~ get {
    entity(as[Foo]) { foo =>
      complete(foo)
    }
  }

  test("must return hello") {
    val (age, name) = ("15", "alex")
    val header = Authorization(OAuth2BearerToken("""{
                                                   |  "brandId": "qwerty",
                                                   |  "user_uuid": "admin",
                                                   |  "department": "dep1"
                                                   |}""".stripMargin))

    Get("/").withHeaders(header) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe s"$name $age"
    }
  }

  test("must return object with value from header") {
    val headers = Seq(HttpHeader.parse("Auth", "{\"value\": \"str\"}"))
      .collect { case ok: ParsingResult.Ok => ok.header }(breakOut)
    Get("/").withHeaders(headers) ~> route ~> check {
      responseAs[Foo] shouldBe Foo("str")
    }
  }

  test("must return object with value from header ignoring sent body") {
    val bar = Bar("hello")
    val headers = Seq(HttpHeader.parse("Auth", "{\"value\": \"str\"}"))
      .collect { case ok: ParsingResult.Ok => ok.header }(breakOut)
    Get("/", bar).withHeaders(headers) ~> route ~> check {
      responseAs[Foo] shouldBe Foo("str")
    }
  }

  test("must return object with value from header overriding sent body") {
    val foo = Foo("hello")
    val headers = Seq(HttpHeader.parse("Auth", "{\"value\": \"str\"}"))
      .collect { case ok: ParsingResult.Ok => ok.header }(breakOut)
    Get("/", foo).withHeaders(headers) ~> route ~> check {
      responseAs[Foo] shouldBe Foo("str")
    }
  }

  test("must return sent object") {
    val foo = Foo("hello")
    Get("/", foo) ~> route ~> check {
      responseAs[Foo] shouldBe Foo("hello")
    }
  }

}
