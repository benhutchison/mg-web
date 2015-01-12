name := "mg-web"

organization := "com.github.benhutchison"

version := "0.1"

scalaVersion := "2.11.2"


libraryDependencies ++= Seq(
  "com.github.benhutchison" %% "mg-domain" % "0.1",
  "com.github.benhutchison" %% "prickle" % "1.1.0",
  "com.lihaoyi" %% "autowire" % "0.2.3",
  "net.databinder" %% "unfiltered" % "0.8.4",
  "net.databinder" %% "unfiltered-filter-async" % "0.8.4",
  "net.databinder" %% "unfiltered-jetty" % "0.8.4"
)

resolvers += "oss snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"

//scalacOptions ++= Seq("-Ymacro-debug-lite")


//scalacOptions ++= Seq("-Xlog-implicits")
