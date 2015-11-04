resolvers ++= Seq(
  "Sonatype OSS Releases"            at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Public Repositories" at "https://oss.sonatype.org/content/groups/public/"
)

val jacksonVersion = "2.6.2"
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

libraryDependencies <++= scalaVersion { scala_version ⇒ Seq("org.scala-lang" % "scala-reflect" % scala_version) }
libraryDependencies ++= slf4j ++ log4j ++ elasticSearch ++ testDependencies
