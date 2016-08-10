resolvers     += Resolver.sonatypeRepo("releases")
updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true)

val log4jVersion     = "[2.0.1,2.9.99]"
val slf4jVersion     = "[1.7.0,1.9.99]"
val elastic4sVersion = "[2.2.0,2.3.99]"
val groovyVersion    = "[2.4.4,2.4.99]"

val slf4j = Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion % "provided",
  "org.slf4j" % "slf4j-ext" % slf4jVersion % "provided"
)

val log4j = Seq(
  "org.apache.logging.log4j" % "log4j-api"        % log4jVersion % "test",
  "org.apache.logging.log4j" % "log4j-core"       % log4jVersion % "test",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion % "test",
  "com.fasterxml.jackson.core"       % "jackson-databind"        % "2.6.3" % "test",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.6.2" % "test" exclude("org.yaml", "snakeyaml")
)

val elasticsearch = Seq(
  "com.sksamuel.elastic4s"   %% "elastic4s-core" % elastic4sVersion % "provided",
  "org.elasticsearch"        %  "elasticsearch"  % elastic4sVersion % "provided",
  "org.elasticsearch.module" %  "lang-groovy"    % elastic4sVersion % "provided",
  "org.codehaus.groovy"      %  "groovy"         % groovyVersion    % "provided" classifier "indy"
)

val postgresql = Seq(
  "com.github.tminglei" %% "slick-pg"  % "0.10.1"/*,
  "com.typesafe.slick"  %% "slick"     % "3.1.0"           % "provided",
  "org.postgresql"      % "postgresql" % "9.4-1205-jdbc42" % "provided"*/
)

val productionDependencies = slf4j ++ elasticsearch ++ postgresql

val testDependencies = log4j ++ Seq(
  "com.sksamuel.elastic4s"  %% "elastic4s-testkit" % elastic4sVersion % "test" notTransitive(),
  "org.elasticsearch"       %  "elasticsearch"     % elastic4sVersion classifier "tests",
  "org.scalatest"           %% "scalatest"         % "3.0.0"          % "test" exclude("org.scala-lang", "scala-reflect")
)

libraryDependencies <+= scalaVersion { scala_version ⇒ "org.scala-lang" % "scala-reflect" % scala_version }
libraryDependencies ++= productionDependencies ++ testDependencies
