name := "SparkInAction"

version := "0.1"

scalaVersion := "2.11.12"

scalacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= {
  val sparkVer = "2.3.0"
  Seq(
    "org.apache.spark" %% "spark-core" % sparkVer,
    "org.apache.spark" %% "spark-streaming" % sparkVer,
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVer,
    "org.apache.spark" %% "spark-sql" % sparkVer,
    "org.apache.spark" %% "spark-mllib" % sparkVer
  )
}
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "2.2.2"
libraryDependencies += "com.stratio.datasource" % "spark-mongodb_2.11" % "0.12.0"