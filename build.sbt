import sbt.Resolver

name := "JwtSnapshot"

version := "0.1"

scalaVersion := "2.12.7"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= {
  val akka_http_version = "10.1.5"
  val circeVersion = "0.10.0"

  Seq(
    "com.typesafe.akka" %% "akka-http" % akka_http_version,
    "com.typesafe.akka" %% "akka-http-testkit" % akka_http_version,
    "org.scalactic"     %% "scalactic" % "3.0.5",
    "org.scalatest"     %% "scalatest" % "3.0.5",
    "de.heikoseeberger" %% "akka-http-circe" % "1.22.0",

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )
}