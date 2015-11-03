import scala.util.Try

name         := "silky-persistence"
organization := "com.github.piltt"
version      := Try(sys.env("BUILD_NUMBER")).map("1.0." + _).getOrElse("1.0-SNAPSHOT")

scalaVersion := "2.10.6"
crossScalaVersions := Seq("2.10.6", "2.11.7")

javacOptions  ++= Seq("-Xms512m", "-Xmx512m", "-Xss4m")
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions")

resolvers ++= Seq(
  "Sonatype OSS Releases"            at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Public Repositories" at "https://oss.sonatype.org/content/groups/public/"
)

graphSettings

val jacksonVersion = "2.4.2"
val log4jVersion   = "2.4.1"
val slf4jVersion   = "1.7.12"

val slf4j = Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-ext" % slf4jVersion
)

val log4j = Seq(
  "org.apache.logging.log4j" % "log4j-api"        % log4jVersion % "test",
  "org.apache.logging.log4j" % "log4j-core"       % log4jVersion % "test",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion % "test",
  "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion % "test",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion % "test"
)

val elasticSearch = Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "[1.7.4,1.7.99]" % "provided",
  "org.elasticsearch"      %  "elasticsearch"  % "[1.7.3,1.7.99]" % "provided"
)

val testDependencies = Seq(
  "com.sksamuel.elastic4s"  %% "elastic4s-testkit" % "[1.7.4,1.7.99]" % "test" notTransitive(),
  "org.scalatest"           %% "scalatest"         % "3.0.0-M10"      % "test"
)

libraryDependencies <++= scalaVersion { scala_version ⇒ Seq(
    "org.scala-lang" % "scala-reflect" % scala_version
  ) ++ slf4j ++ log4j ++ elasticSearch ++ testDependencies
}

publishTo <<= version { project_version ⇒
  val nexus = "https://oss.sonatype.org/"
  if (project_version.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ ⇒ false }

credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", System.getenv("SONATYPE_USER"), System.getenv("SONATYPE_PASSWORD"))

pomExtra :=
  <url>https://github.com/PILTT/silky-persistence</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git://github.com/PILTT/silky-persistence.git</url>
    <connection>scm:git://github.com/PILTT/silky-persistence.git</connection>
  </scm>
  <developers>
    <developer>
      <id>franckrasolo</id>
      <name>Franck Rasolo</name>
      <url>https://github.com/franckrasolo</url>
    </developer>
  </developers>
