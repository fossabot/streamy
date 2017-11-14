name := name.value + "-core"

maintainer := "Adrien Mannocci <adrien.mannocci@gmail.com>"

// Dependencies version
lazy val akkaVersion = "2.5.6"
lazy val circeVersion = "0.8.0"
lazy val logbackVersion = "1.2.3"
lazy val logbackContribVersion = "0.3.0"
lazy val commonsLangVersion = "3.6"
lazy val jacksonVersion = "2.9.1"
lazy val metricsScalaVersion = "3.5.9"
lazy val metricsJvmVersion = "3.2.3"
lazy val guavaVersion = "23.3-jre"
lazy val diffsonVersion = "2.2.2"

// Custom resolvers
resolvers ++= Seq(
  "Techcode" at "https://nexus.techcode.io/repository/maven-public"
)

// All akka libraries
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor",
  "com.typesafe.akka" %% "akka-testkit",
  "com.typesafe.akka" %% "akka-stream",
  "com.typesafe.akka" %% "akka-slf4j"
).map(_ % akkaVersion % Compile) ++ Seq(
  "com.typesafe.akka" %% "akka-testkit",
  "com.typesafe.akka" %% "akka-stream-testkit"
).map(_ % akkaVersion % Test)

// All circe libraries
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

// All other libraries
libraryDependencies ++= Seq(
  "org.gnieh" %% "diffson-circe" % diffsonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "org.apache.commons" % "commons-lang3" % commonsLangVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "io.techcode.logback.contrib" % "logback-json-layout" % logbackContribVersion,
  "com.google.guava" % "guava" % guavaVersion,
  "nl.grons" %% "metrics-scala" % metricsScalaVersion,
  "io.dropwizard.metrics" % "metrics-jvm" % metricsJvmVersion
)

// Jmh settings
sourceDirectory in Jmh := new File((sourceDirectory in Test).value.getParentFile, "bench")
classDirectory in Jmh := (classDirectory in Test).value
dependencyClasspath in Jmh := (dependencyClasspath in Test).value

// Debian packaging
packageSummary := "Streamy Package"
packageDescription := "Transport and process your logs, events, or other data"

// Enable some plugins
enablePlugins(JavaAppPackaging, JmhPlugin)