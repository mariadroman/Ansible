name := "digest"

version := "1.0.0"

scalaVersion := "2.11.8"
val sparkVersion = "2.2.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
//  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion force(),
  "org.apache.spark" %% "spark-streaming" % sparkVersion force,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion force,
  "com.typesafe" % "config" % "1.3.1"
)

// ------------------------------
// ASSEMBLY
assemblyJarName in assembly := s"${name.value}-fat.jar"

// Add exclusions, provided...
assemblyMergeStrategy in assembly := {
  {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
