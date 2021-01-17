// import AssemblyKeys._
// assemblySettings
name := "jamr"
version := "0.1-SNAPSHOT"
organization := "edu.cmu.lti.nlp"
scalaVersion := "2.12.1"
crossScalaVersions := Seq("2.11.8","2.11.12", "2.12.4","2.10.6")
libraryDependencies ++= Seq(
"com.jsuereth" %% "scala-arm" % "2.0",
"edu.stanford.nlp" % "stanford-corenlp" % "3.4.1",
"edu.stanford.nlp" % "stanford-corenlp" % "3.4.1" classifier "models",
"org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
"org.scala-lang.modules" %% "scala-pickling" % "0.10.1"
// "org.scala-lang" % "scala-swing" % "2.10.3"
)