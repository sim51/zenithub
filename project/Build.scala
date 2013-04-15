import sbt._
import Keys._

import play.Project._

object ApplicationBuild extends Build {

    val appName         = "Zenithub"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "securesocial" %% "securesocial" % "master-SNAPSHOT",
      "org.neo4j" % "neo4j" % "1.9.RC1",
      "org.neo4j.app" % "neo4j-server" % "1.9.RC1" classifier "static-web" classifier "" exclude("org.slf4j", "slf4j-jdk14"),
      "com.sun.jersey" % "jersey-core" % "1.9",
      "ch.qos.logback" % "logback-core" % "1.0.3" force(), // this should override the Play version
      "ch.qos.logback" % "logback-classic" % "1.0.3" force(),
      jdbc,
      anorm
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += Resolver.url("sbt-plugin-snapshots", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("sbt-plugin-release", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("typesafe-release", url("http://repo.typesafe.com/typesafe/releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("neo4j-release", url("http://m2.neo4j.org/content/repositories/releases"))(Resolver.mavenStylePatterns),
      resolvers += Resolver.url("neo4j-public-repository", url("http://m2.neo4j.org/content/groups/public"))(Resolver.mavenStylePatterns)
    )

}
