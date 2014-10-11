name := "mg-web"

organization := "com.github.benhutchison"

version := "0.1"

scalaVersion := "2.11.2"


libraryDependencies ++= Seq(
  "com.github.benhutchison" %% "mg-domain" % "0.1",
  "com.github.benhutchison" %% "prickle" % "1.0.3",
  "com.lihaoyi" %% "autowire" % "0.2.3",
  "net.databinder" %% "unfiltered" % "0.8.2",
  "net.databinder" %% "unfiltered-filter" % "0.8.2",
  "net.databinder" %% "unfiltered-jetty" % "0.8.2"
)

//scalacOptions ++= Seq("-Ymacro-debug-lite")


//scalacOptions ++= Seq("-Xlog-implicits")