import scala.util.Try
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name         := "silky-persistence"
organization := "com.github.piltt"
version      := Try(sys.env("BUILD_NUMBER")).map("1.0." + _).getOrElse("1.0-SNAPSHOT")

val scala_2_11 = "2.11.8"
scalaVersion := scala_2_11
crossScalaVersions := Seq("2.10.6", scala_2_11)

javacOptions  ++= Seq("-Xms512m", "-Xmx512m", "-Xss4m")
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions")

coverageHighlighting := false
graphSettings
