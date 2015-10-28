name := "elevator"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"     % "2.1.6",
  "com.typesafe.akka" %% "akka-actor"    % "2.4.0",
  "com.typesafe.akka" %% "akka-testkit"  % "2.4.0" % "test"
  )
