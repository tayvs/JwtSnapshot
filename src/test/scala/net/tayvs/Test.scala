package net.tayvs

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import org.scalatest.{FunSuite, Matchers}
import pdi.jwt.Jwt

class Test extends FunSuite with Matchers with ScalatestRouteTest
  with CirceCustomMarshaller with CustomParamExtractor
  with ErrorAccumulatingCirceSupport with JwtParser {

  override def jwtParser: JwtParser = new JwtParser

  val route: Route = get {
    pathEndOrSingleSlash {
      parameter(BrandId, UserUUID) { (name, userUUID) =>
        complete(s"$name $userUUID")
      }
    }
  } ~ post {
    pathEndOrSingleSlash {
      println("post /")
      entity(as[CreateRequest]) { request =>
        complete(request)
      }
    }
  }

  val jwtToken: String = Jwt.encode(
    """{
      |  "brandId": "qwerty",
      |  "user_uuid": "admin",
      |  "department": "dep1"
      |}""".stripMargin)

  test("must return hello") {
    val header = Authorization(OAuth2BearerToken(jwtToken))

    Get("/").withHeaders(header) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "qwerty admin"
    }
  }

  test("must return object with value from header") {
    val header = Authorization(OAuth2BearerToken(jwtToken))
    val body = Map("content" -> "qq")


    Post("/", body).withHeaders(header) ~> route ~> check {
      responseAs[CreateRequest] shouldBe CreateRequest("qwerty", "qq")
    }
  }

  test("must return object with value from header overriding sent body") {
    val body = Map("brandId" -> "MyBrandId", "content" -> "qq")
    val header = Authorization(OAuth2BearerToken(jwtToken))

    Post("/", body).withHeaders(header) ~> route ~> check {
      responseAs[CreateRequest] shouldBe CreateRequest("qwerty", "qq")
    }
  }

}
