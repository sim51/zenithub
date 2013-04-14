import sbt._
import Keys._

import play.Project._

object ApplicationBuild extends Build {

    val appName         = "Zenithub"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "securesocial" %% "securesocial" % "master-SNAPSHOT",
      jdbc,
      anorm
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += Resolver.url("sbt-plugin-snapshots", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("sbt-plugin-release", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("typesafe-release", url("http://repo.typesafe.com/typesafe/releases/"))(Resolver.ivyStylePatterns)
    )

}
