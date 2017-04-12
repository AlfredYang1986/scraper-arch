lazy val commonSettings = Seq(
    organization := "com.blackmirror",
    version := "1.0",
    scalaVersion := "2.11.8"
)

ideaExcludeFolders += ".idea"
ideaExcludeFolders += ".idea_modules"

libraryDependencies ++= Seq(

    // akka
    "com.typesafe.akka" %% "akka-actor" % "2.4.17",
    "com.typesafe.akka" %% "akka-agent" % "2.4.17",
    "com.typesafe.akka" %% "akka-camel" % "2.4.17",
    "com.typesafe.akka" %% "akka-cluster" % "2.4.17",
    "com.typesafe.akka" %% "akka-cluster-metrics" % "2.4.17",
    "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.17",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.17",
    "com.typesafe.akka" %% "akka-contrib" % "2.4.17",
    "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.4.17",
    "com.typesafe.akka" %% "akka-osgi" % "2.4.17",
    "com.typesafe.akka" %% "akka-persistence" % "2.4.17",
    "com.typesafe.akka" %% "akka-persistence-tck" % "2.4.17",
    "com.typesafe.akka" %% "akka-remote" % "2.4.17",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
    "com.typesafe.akka" %% "akka-stream" % "2.4.17",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.17",
    "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.17",
    "com.typesafe.akka" %% "akka-typed-experimental" % "2.4.17",
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.17",

    // akka-http
    "com.typesafe.akka" %% "akka-http-core" % "10.0.1",
    "com.typesafe.akka" %% "akka-http" % "10.0.1",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.0.1",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.1",
    "com.typesafe.akka" %% "akka-http-jackson" % "10.0.1",
    "com.typesafe.akka" %% "akka-http-xml" % "10.0.1",

    // jsoup
    "org.jsoup" % "jsoup" % "1.10.2" from "https://jsoup.org/packages/jsoup-1.10.2.jar",

    // mongodb
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.2.1",
    "org.mongodb.spark" %% "mongo-spark-connector" % "2.0.0",
    "org.mongodb" % "casbah-core_2.11" % "3.1.1",
	"org.mongodb" % "casbah_2.11" % "3.1.1",

    // spark
    "org.apache.spark" %% "spark-core" % "2.0.0",
    "org.apache.spark" %% "spark-sql" % "2.0.0",

    // xml
    "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.6",

    // json
    "com.typesafe.play" % "play-json_2.11" % "2.5.0-M2"
)

lazy val root = (project in file(".")).
    settings(commonSettings: _*).
    settings(
        name := "scraper-arch",
        fork in run := true,
        javaOptions += "-Xmx4G"
    )
