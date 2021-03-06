package com.freeman

import java.text.SimpleDateFormat

import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}
import org.apache.spark.streaming._

import scala.io.Source.fromFile
import org.apache.spark.sql._
import org.apache.spark.sql.functions.broadcast

import scala.collection.mutable
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties

import com.mongodb.spark.sql.helpers.StructFields
import org.apache.hadoop.mapred.lib

import scala.collection.mutable.ListBuffer

//import com.freeman.StreamingLogAnalyzer.{brokerList, checkpointDir, logsTopic, numberPartitions, statsTopic, _}
import com.mongodb.spark.MongoSpark
import org.bson.Document
import com.mongodb.spark.config.WriteConfig
import org.apache.spark.sql.types._
import com.stratio.datasource.mongodb.config.{MongodbConfig, MongodbCredentials, MongodbSSLOptions}
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.functions.to_json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex
import scala.util.{Failure, Success}
import org.apache.spark.sql.types._
import scala.collection.mutable
import com.stratio.datasource.mongodb.config.{MongodbConfig, MongodbCredentials, MongodbSSLOptions}
import scala.annotation.tailrec
import org.apache.kafka.common.serialization.Serializer

import org.apache.spark.sql.expressions.Window
object LoginCount {

  def currentActiveExecutors(sc: SparkContext): Seq[String] = {
    val allExecutors = sc.getExecutorMemoryStatus.map(_._1)
    val driverHost: String = sc.getConf.get("spark.driver.host")
    allExecutors.filter(! _.split(":")(0).equals(driverHost)).toList
  }

  def setupStreamContext(checkpointDirectory: String): StreamingContext = {

    val conf = new SparkConf().setAppName("Kafka-Spark-Kafka")
    conf.setIfMissing("spark.master", "spark://10.13.11.42:7077")
    conf.set("spark.streaming.backpressure.enabled","true")
    conf.set("spark.streaming.kafka.maxRatePerPartition", "200")
    //conf.set("spark.streaming.concurrentJobs", "4")
    conf.set("spark.executor.heartbeatInterval", "20")
    conf.set("spark.streaming.kafka.consumer.cache.enabled", "false")
    conf.set("spark.scheduler.mode", "FAIR")
    conf.set("spark.streaming.receiver.writeAheadLog.enable", "true")
    // .config("spark.mongodb.output.uri", "mongodb://anpr:vp9anpr@10.12.11.82:27017/anpr")
    val spark = SparkSession
    .builder()
    .config(conf)
    .config("spark.mongodb.output.uri", "mongodb://10.13.11.188:27017/datamap")
    .getOrCreate()
    val sc = spark.sparkContext
    spark.sparkContext.setLocalProperty("spark.scheduler.pool", "production")

    /*val spark = SparkSession.builder()
      .master("spark://10.13.11.42:7077")
      .appName("Kafka-Spark-Kafka")
      //.config("spark.scheduler.mode", "FAIR")
      //.config("spark.mongodb.output.uri", "mongodb://10.13.11.42:27017/anpr")
      //.config("spark.mongodb.output.uri", "mongodb://anpr:vp9anpr@10.12.11.82:27017/anpr")
      .getOrCreate()*/

    val ssc = new StreamingContext(sc, Durations.seconds(5))
    ssc.checkpoint(checkpointDirectory)
    //vp9DataMapStreaming(ssc)
    //ch13Realtime(ssc, spark)
    //testCh13Realtime(ssc, spark)
    //ch3(spark)
    ch5(spark, sc)
    //mergeData(spark)
    ssc
  }



  def main(args: Array[String]): Unit ={



    //val sparkConf = new SparkConf().setAppName("SparkInAction-Ch6").setMaster("spark://10.13.11.42:7077")
    //val ssc = new StreamingContext(sparkConf, Durations.seconds(1))

    /*val conf = new SparkConf().setAppName("Kafka-Spark-Kafka")
    conf.setIfMissing("spark.master", "spark://10.13.11.42:7077")
    conf.set("spark.streaming.backpressure.enabled","true")
    conf.set("spark.streaming.kafka.maxRatePerPartition", "200")
    //conf.set("spark.streaming.concurrentJobs", "4")
    conf.set("spark.executor.heartbeatInterval", "20")
    conf.set("spark.streaming.kafka.consumer.cache.enabled", "false")
    conf.set("spark.scheduler.mode", "FAIR")
    // .config("spark.mongodb.output.uri", "mongodb://anpr:vp9anpr@10.12.11.82:27017/anpr")

    val spark = SparkSession
      .builder()
      .config(conf)
      .config("spark.mongodb.output.uri", "mongodb://10.13.11.188:27017/datamap")
      .getOrCreate()

    /*val spark = SparkSession.builder()
      .master("spark://10.13.11.42:7077")
      .appName("Kafka-Spark-Kafka")
      //.config("spark.scheduler.mode", "FAIR")
      //.config("spark.mongodb.output.uri", "mongodb://10.13.11.42:27017/anpr")
      //.config("spark.mongodb.output.uri", "mongodb://anpr:vp9anpr@10.12.11.82:27017/anpr")
      .getOrCreate()*/

    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Durations.seconds(5))
    spark.sparkContext.setLocalProperty("spark.scheduler.pool", "production")*/

    // Get StreamingContext from checkpoint data or create a new one
    val checkpointDir = "hdfs://10.13.11.42:9000/home/freeman/spark_in_action"
    val ssc = StreamingContext.getOrCreate(checkpointDir, () =>  setupStreamContext(checkpointDir))
//    vp9DataMapStreaming(ssc)
    //vp9DataMap(ssc, spark, sc)
    //ch13Realtime(ssc, spark)
    //ch6Kafk(ssc, spark)
    //ch6(ssc, sparkConf)
    //ch3(spark)
    //ch4(spark)
    ssc.start()
    ssc.awaitTermination()
  }


  def vp9DataMapStreaming(context: StreamingContext): Unit ={
    ///home/freeman/bigdata/data/datamapVP9/input
//    context.checkpoint("hdfs://10.13.11.42:9000/home/freeman/spark_in_action")
    val topicList = List("license_plate_topic")
    val destinationTopic="elasticsearch-data"
    val props = new Properties()
    props.put("bootstrap.servers", "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")


    val mongoDbDatabase = "datamap"
    val mongoDbCollection = "test"
    val MongoDbOptions = Map(MongodbConfig.Host -> "10.13.11.188:27017", MongodbConfig.Database -> mongoDbDatabase, MongodbConfig.Collection -> mongoDbCollection)

    val kafkaBrokers = "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094" // comma separated list of broker:host
    val group = "spark-streaming-consumer"
    val kafkaParam = new mutable.HashMap[String, String]()
    kafkaParam.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    kafkaParam.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    kafkaParam.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParam.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    val messageStream = KafkaUtils.createDirectStream(context,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicList, kafkaParam))


    //val fileStream = context.textFileStream("file:////home/freeman/bigdata/data/datamapVP9/input")
    //val tranData = fileStream.map(_.split(","))

    /*val transDatamap = tranData.map(tran => (tran(5).toInt, List((tran(10).toInt, tran(8), tran(9), tran(3)))))
    val tmp = transDatamap.reduceByKey((arr1, arr2) => {
      arr1 ++ arr2
    })*/

    case class Datamap(channelId: Long, cameraId: Long, start: String, end: String, status: Int)
    /*val datamap = fileStream.flatMap(line => {
      val s = line.split(",")
      try {
        assert(s(10).toInt == 0 || s(10).toInt == 1)
        List(Datamap(s(5).toLong, s(3).toLong, s(8), s(9), s(10).toInt))
      }
      catch {
        case e : Throwable => println("Wrong line format ("+e+"): "+line)
          List()
      }
    })*/

    val datamap = messageStream.flatMap(line => {

      val s = line.value().split(",")
      try {
        assert(s(10).toInt == 0 || s(10).toInt == 1)
        List(Datamap(s(5).toLong, s(3).toLong, s(8), s(9), s(10).toInt))
      }
      catch {
        case e : Throwable => println("Wrong line format ("+e+"): "+line)
          List()
      }
    })

    datamap.checkpoint(Durations.seconds(5))
    //val dataMapPerChannelId = datamap.map(datamap => (datamap.channelId, datamap.cameraId, datamap.start, datamap.end, datamap.status))
    val dataMapPerChannelId = datamap.map(datamap => (datamap.channelId, List((datamap.channelId, datamap.cameraId, datamap.start, datamap.end, datamap.status))))//.filter(a => {a._1 == 15386})
    /*val dataMapPerChannelId = datamap.map(datamap => (datamap.channelId, List((datamap.channelId, datamap.cameraId, datamap.start, datamap.end, datamap.status)))).reduceByKey((e1, e2) => {
      (e1 ++ e2)
    })*/

   /* dataMapPerChannelId.foreachRDD(f => {
      f.collect().foreach(a => {
        println(a._1 + "---" + a._2.mkString(" |+| "))
      })
    })*/

    /*dataMapPerChannelId.foreachRDD(f => {
      f.filter(b => {b._1 == 15386}).collect().foreach(a => {
        println(a._1 + "---" + a._2.mkString(" |+| "))
      })
    })*/

    //DStream[(Long, Long, String, String, Int)]
    /*dataMapPerChannelId.foreachRDD(perChannel => {
      println(perChannel.collect().foreach(a => {
        println(a._1 + "--------" + a._2)
      }))
    })*/

  /*  def updateAmountState(channelId: Long, data: Option[Seq[Datamap]], state: State[Datamap]): Option[Datamap] = {

      var total = data.getOrElse()
      if(state.exists()){
        total = total :+ state.get()
      }
      state.update(total)
    }*/

    /*//Long, amount: Option[Double], state: State[Double]) =>
    val updateAmountState1 = (time: Time, channelId: Long, amount: Option[List[(Long, Long, String, String, Int)]], state: State[List[(Long, Long, String, String, Int)]]) =>{
      var total = amount.getOrElse(List())
      if(state.exists()){
        total = total ++ state.get()
      }
      if(state.isTimingOut()){
        println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      }else{
        state.update(total)
      }
      //println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      //state.update(total)
      Some((channelId, total.reverse))
    }*/

    val updateAmountState1 = (time: Time, channelId: Long, amount: Option[List[(Long, Long, String, String, Int)]], state: State[List[(Long, Long, String, String, Int)]]) =>{
      var total = amount.getOrElse(List())
      var shouldRemove = false
      var tmp: List[(Long, Long, String, String, Int)] = List()

      if(state.exists()){
        val currentState = state.get()
        println()
        println("<----------State Is Exists-----------> ")
        println("STATE---------------------> " + state) //3
        println("Total ----------------->  " + total)
        tmp = total
        println("Tmp Total ----------------->  " + tmp)
        if(!total.isEmpty && currentState.head._5 == total.head._5 && currentState.head._4 == total.head._3){
          total = total ++ currentState
          println("---------------------------------> Total1 match with conditional: " + total)
        }else{
          println("---ELSE_currentState  " + currentState)
          println("---ELSE_total  " + total)
          total = List((currentState.head._1, currentState.head._2, currentState.last._3, currentState.head._4, currentState.head._5))
          shouldRemove = true
        }


        println("---------------State-Remove: " + state)
        println("---------------------------------> Total - Again: " + total)
      }else{
        println("State is not exists --->State " + state)
        println("State is not exists --->Total " + total)
        println("State is not exists --->Amount " + amount)
        //state.update(List(total.head))
      }

      if(state.isTimingOut()){

        println("++++++++++++++++++++++++++++++++Is TimingOut", state.toString() + " _______ " + state.getOption())
        new KafkaProducer[String,String](props).send(new ProducerRecord[String, String](destinationTopic, "myKey", state.get().mkString(" |+| ")))
        state.update(state.get())
      }else{
        println()
        println("<----------State Update----------> ")
        println("---------------------------------> Total2: " + total) //1
        println("---------------State: " + state)
        state.update(total)
      }
      println()
      println("<----------State Some----------> ")

      if(shouldRemove){

        println("shouldRemove--------------------> " + shouldRemove)
        println("---------------------------------> SOMMMMMMMMMMMMMMMMMMME: " + Some((channelId, total))) //2
        state.update(tmp)
        new KafkaProducer[String,String](props).send(new ProducerRecord[String, String](destinationTopic, "myKey", total.mkString(" |+| ")))
        Some((total))
      }else{
        println("shouldRemove--------------------> " + shouldRemove)
        Some()
      }
    }

    //ssc.mapWithState(spec).filter(!_.isEmpty).foreachRDD(

    val spec = StateSpec.function(updateAmountState1).timeout(Durations.seconds(15)) //StateSpec[Long, List[(Long, Long, String, String, Int)]

  //  val amountState = dataMapPerChannelId.mapWithState(spec).stateSnapshots()

    val amountState = dataMapPerChannelId.mapWithState(spec).stateSnapshots().filter(a => {a.isInstanceOf}).foreachRDD(rdd => {
     /* import sparkSession.implicits._
      val newRdd = rdd.map(rawData => {
        (rawData._2)
      })
      newRdd.foreach(data => {
        val dataMapDF = data.toDF("channelid", "cameraid", "start", "end", "status")
        dataMapDF.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
      })*/



      /*rdd.foreachPartition((iter) => {
        import sparkSession.implicits._
        iter.foreach(data => {
          val dataMapDF = data._2.toDF("channelid", "cameraid", "start", "end", "status")
          dataMapDF.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
        })
      })*/


      //if(!rdd.isEmpty()){

        rdd.collect().foreach(data => {

          println(data._1 + " ------------- " + data._2.mkString(" |+| "))

          //val dataMapDF = data._2.toDF("channelid", "cameraid", "start", "end", "status")
          //dataMapDF.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
        })
      //}
    })

    /*val tmp = amountState.map(f =>
      (f._2)).foreachRDD(a => {
        import sparkSession.implicits._
        a.foreach(b => {
          b.toDF("channelid", "cameraid", "start", "end", "status").write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
        })
    })*/
/*
    amountState.foreachRDD({ rdd =>
      //import sparkSession.implicits._
      if(!rdd.isEmpty()){
        rdd.foreach(data => {
          //data._2.toDF("channelid", "cameraid", "start", "end", "status").show(20, false)
          println(data._2.mkString(" | "))
        })
      }
      //val dataMapDF = data._2.toDF("channelid", "cameraid", "start", "end", "status")
      //dataMap.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
    })*/
  }


  def vp9DataMap(ssc: StreamingContext, sparkSession: SparkSession, sc: SparkContext): Unit = {

    val mongoDbDatabase = "datamap"
    val mongoDbCollection = "test"
    val MongoDbOptions = Map(MongodbConfig.Host -> "10.13.11.188:27017", MongodbConfig.Database -> mongoDbDatabase, MongodbConfig.Collection -> mongoDbCollection)
    val tranFile = sc.textFile("file:///home/freeman/bigdata/data/datamapVP9/datamap.csv") // RDD[String]
    val tranData = tranFile.map(_.split(",")) // RDD[Array[String]]
    case class DataMap(timeStart:String, timeEnd: String){}
    //val transByCust = tranData.map(tran => (tran(5).toInt, List(tran(10) -> new DataMap(tran(8), tran(9))))) // RDD[(Int, Array[String])]
    val transByCust = tranData.map(tran => (tran(5).toInt, List((tran(10).toInt, tran(8), tran(9), tran(3))))) // RDD[(Int, Array[String])]

    val tmp = transByCust.reduceByKey((arr1, arr2) => {
      arr1 ++ arr2
    })


  /* tmp.collect.foreach(f => {

     println(f._1 + "__" + f._2.toList)
   })*/
    //transByCust.keys.distinct().count()
    //println("Keys_Distinct_Count", transByCust.keys.distinct().count())
    //println("Transaction By Key", transByCust.countByKey())
    //val group = tmp.groupBy(_._1).map(p => p._1 -> p._2.map(_._2))

    def groupSameKeyV4[A, B](xs: List[(A, B)]): List[(A, List[B])] = {
      def rec(xs: List[(A, B)], accum: List[(A, List[B])]): List[(A, List[B])] =
        (xs, accum) match {
          case (Nil, _) => {
            println("#_3")
            println(accum)
            accum.map{ case (a, bs) => (a, bs.reverse)}.reverse
          }
          case ((a, b) :: xss, (lastkey, lastvalues) :: acctail) if a == lastkey => {
            println("#_2")
            println("(a,b): " + (a, b))
            println("xss " + xss)
            println("acctail: " + acctail)
            println("(lastkey, lastvalues) " + (lastkey, lastvalues))
            println("(a, b :: lastvalues) :: acctail " + (a, b :: lastvalues) :: acctail)
            rec(xss, (a, b :: lastvalues) :: acctail)
          }
          case ((a, b) :: xss, _) => {
            println("#_1")
            println("(a,b) " + (a,b))
            println("xss " + xss)
            println("accum " + accum)
            rec(xss, (a, List(b)) :: accum)
          }
        }
      rec(xs, Nil)
    }

    def mergeWithTimestamp(xs: List[(String, String, String, Int, Int)], accum: List[(String, String, String, Int, Int)]): List[(String, String, String, Int, Int)] =
      (xs, accum) match {
        case (Nil, _) => {
          accum.map{ case (a, b, c, d, e) => (a, b, c, d, e)}.reverse
        }
        case((a, b, c, d, e) :: xss, (e1, e2, e3, e4, e5) :: accTail) if a != e2 => {
          mergeWithTimestamp(xss, (a, b,c, d, e) :: accum)
        }
        case ((a, b, c, d, e) ::  xss, (e1, e2, e3, e4, e5) :: accTail) if a == e2 => {
          mergeWithTimestamp(xss, (e1, e2, e3, e4, e5).copy(_2 = b) :: accTail)
        }
        case ((a, b, c, d, e) :: xss, Nil) => {
          mergeWithTimestamp(xss, (a, b, c, d, e) :: accum)
        }
      }

    val rss = tmp.map(f => {
      //(f._1, groupSameKeyV2(f._2, List((f._2.head._1, List((f._2.head._2, f._2.head._3, f._2.head._4))))))
      (f._1, groupSameKeyV4(f._2.map{ case(a,b,c,d) => (a,(b,c,d,a, f._1)) }))
      //(f._1, groupSameKeyV4(f._2.map(d => (d._1, (d._2, d._3, d._4, d._1, f._1)))))
    })

    rss.collect.foreach(f => {
      println(f._1 + "---" + f._2.foreach(a => {
        println(a._1 + "---" + a._2.mkString(" +|+ "))
      }))
    })
  val rss2 = rss.map(f => {

      (f._1, f._2.map(a => {
        (a._1, mergeWithTimestamp(a._2, List()))
      }))
   })
    //RDD[(Int, List[(Int, List[(String, String, String)])])]

    val rss3 = rss2.map(f => {
      (f._2.map(a => {
        (a._2.map(b => {
          (b._1, b._2, b._3, b._4, b._5)
        }))
      }))
    })

    rss3.collect.foreach(f => {
      //printf(f.mkString(" +|+ "))
      f.foreach(item => {
        import sparkSession.implicits._
        //item.toDF("start", "end", "cameraid", "status", "channelid").show()
        var dataMapDF = item.toDF("start", "end", "cameraid", "status", "channelid")
        dataMapDF.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
      })
    })

    //List((1514415600,1514422800,15136,0,15386), (1514422801,1514469600,15136,0,15386), (1514566800,1514567140,15136,0,15386))

    /*println("-----------rss: " + rss.count())
    rss.collect.foreach(f => {
      println("------RSS-f._1: " + f._1 + " | " + f._2.length)
      println(f._1 + "--" + f._2.mkString(" +|+ "))
    })*/

    /*println("-----------rss: " + rss2.count())
    import sparkSession.implicits._
    rss3.collect.foreach(f => {
      println("------RSS-f._1: " + f._1 + " | " + f._2.length)
      println(f._1 + "--" + f._2.mkString("  +||+  "))

      val tmp = f._2.toDF("col1", "col2")

    })*/

    /*rss3.foreachPartition((iter) => {
      iter.foreach(f => {
        f.foreach(item => {
          import sparkSession.implicits._
          item.toDF("start", "end", "cameraid", "status", "channelid").show()
        })
        //import sparkSession.implicits._
        //val dataSetRecord = sparkSession.createDataset(List[])
        //println("+++++++++++++++++++++>", f._3)
        //val tmp = Seq("""{"camera_id":"22483","encoded_plate_image":"encoded_plate_img_1",
        // "location":"STMC-D985C5","location_x":"329","mode":"0","site_member_id":1111,"site_owner_id":9135,"speed":0,"timestamp":"2018-06-30T07:28:59.086Z","vehicle_plate":"49Y-63447"}""")
        // .toDS()
        //val licensePlateDf=sparkSession.read.schema(schema).json(tmp) //DataFrame
        //licensePlateDf.printSchema()
        //licensePlateDf.show(5)
        //licensePlateDf.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()
      })
    })*/

    //import sparkSession.implicits._
    //val dataSetRecord = sparkSession.createDataset(rss2) // Dataset[String]
    //dataSetRecord.printSchema()
    //dataSetRecord.show(10, true)
    //val licensePlateDf = sparkSession.read.schema(schema).json(dataSetRecord) //DataFrame

    /*rss.foreach(f => {
      //println("------RSS-f._1: " + f._1 + " | " + f._2.length)
      println(f._1 + "--" + f._2.foreach(a => {
        println(a._1 + " && " + a._2.mkString(" ||| "))
      }))
    })*/

    /*val o = tmp.collectAsMap()
    val rss = o.map(f => {
      (f._1, groupSameKeyV2(f._2, List((f._2.head._1, List((f._2.head._2, f._2.head._3))))))
    })

    rss.foreach(f => {
      println(f._1 + "--" + f._2.foreach(a => {
        println(a._1 + " & " + a._2.mkString(" | "))
      }))
    })*/


  }

  case class KafkaProducerWrapper(brokerList: String) {
    val props = new Properties()
    props.put("bootstrap.servers", "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

    /*val producerProps = {
      val prop = new Properties
      prop.put("metadata.broker.list", brokerList)
      prop
    }*/
    //val p = new Producer[Array[Byte], Array[Byte]](new ProducerConfig(producerProps))
    val p = new KafkaProducer[Array[Byte], Array[Byte]](props)
    def send(topic: String, key: String, value: String) {

      //p.send(new KeyedMessage(topic, key.toCharArray.map(_.toByte), value.toCharArray.map(_.toByte)))
      //val tmp = value.toCharArray.map(_.toByte)
      //p.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, key.toCharArray.map(_.toByte), value.toCharArray.map(_.toByte)))
      //p.send(new ProducerRecord[String, String](topic, key, value))
      //val tmp = key.getBytes()
      //val tmp2 = value.getBytes()
      //val ipaddr = Array[Byte](192.toByte, 168.toByte, 1, 9)

      p.send(new ProducerRecord[Array[Byte],Array[Byte]](topic, key.toCharArray.map(_.toByte), value.toCharArray.map(_.toByte)))
    }
  }

  object KafkaProducerWrapper {
    var brokerList = ""
    lazy val instance = new KafkaProducerWrapper("")
  }

  def testCh13Realtime(ssc: StreamingContext, sparkSession: SparkSession ): Unit ={

    //used for connecting to Kafka
    var brokerList: Option[String] = None
    //Spark checkpoint directory
    val checkpointDir: String = "hdfs://10.13.11.42:9000/home/freeman/spark_in_action"
    //Kafka topic for reading log messages
    var logsTopic: Option[String] = Some("weblogs")
    //Kafka topic for writing the calculated statistics
    var statsTopic: Option[String] = Some("stats")
    //Session timeout in milliseconds
    var SESSION_TIMEOUT_MILLIS = 2 * 60 * 1000 //2 minutes
    //Number of RDD partitions to use
    var numberPartitions = 3

    val kafkaReceiverParams = Map[String, String](
      "metadata.broker.list" -> "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094")

    val kafkaBrokers = "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094"   // comma separated list of broker:host
    val group = "realtime"
    val kafkaParam = new mutable.HashMap[String, String]()
    kafkaParam.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    kafkaParam.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    kafkaParam.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParam.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

    val kafkaStream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](List("weblogs"), kafkaParam))

    //2018-08-20 15:24:30.621 192.168.0.171 692fc808-f942-492f-9e50-b495109b7b70 sia.org/ads/2/234/clickfw GET 404 500
    //2018-08-20 15:24:30.729 192.168.0.113 4baee20f-49f5-4b7d-917f-1569e5098a5c sia.org/ads/1/123/clickfw GET 200 500

    case class Logline(time: Long, ipAddr: String, sessionID: String, url: String, method: String, responseCode: Int, responseTime: Int)
    //responseTime: Time it took the server to respond to the request

    val df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")

    val logStream = kafkaStream.flatMap(consumerRecord => {
      val fields = consumerRecord.value().split(" ")
      try{
        List(Logline(df.parse(fields(0) + " " + fields(1)).getTime, fields(2), fields(3), fields(4), fields(5), fields(6).toInt, fields(7).toInt))
      }catch{
        case e: Exception => {
          System.err.println("Wrong line format: " + consumerRecord + " -EXCEPTION- " + e)
          List()
        }
      }
    })

    val logLinePerSecond = logStream.map(ls => ( (ls.time / 1000) * 1000, ls ))

    val reqPerSecond = logLinePerSecond.combineByKey(
        initialCombinerLogline => 1L,
        (mergeValue: Long, logLine: Logline) => mergeValue + 1,
        (mergeCombiner1: Long, mergeCombiner2: Long) => mergeCombiner1 + mergeCombiner2,
        new HashPartitioner(numberPartitions),
        true
    )

    val errorPerSecond = logLinePerSecond.filter(logLine => {
      val resCode = logLine._2.responseCode / 100
      resCode == 4 || resCode == 5
    }).combineByKey(
        initialCombinerLogLine => 1L,
        (mergeValue: Long, logLine: Logline) => mergeValue + 1,
        (mergeCombiner1: Long, mergeCombiner2: Long) => mergeCombiner1 + mergeCombiner2,
        new HashPartitioner(numberPartitions),
        true
    )

    val adUrlPattern = new Regex(".*/ads/(\\d+)/\\d+/clickfw", "adtype")      // urlmatch.group will get data into first () | (\\d+)
    val adsPerSecondAndType = logLinePerSecond.flatMap(logLine => {
      adUrlPattern.findFirstMatchIn(logLine._2.url) match {
        case Some(urlMatch) => List( ((logLine._1, urlMatch.group("adtype")), logLine._2) )
        case None => List()
      }
    }).combineByKey(  //this combineByKey counts all LogLines per timestamp and ad category
        initialCombiner => 1.asInstanceOf[Long],
        (mergeValue: Long, logLine: Logline) => mergeValue + 1,
        (mergeCom1: Long, mergeCom2: Long) => mergeCom1 + mergeCom2,
        new HashPartitioner(numberPartitions),
        true
    )

    val maxTimeBySession = logStream.map(logLine => (logLine.sessionID, logLine.time)).reduceByKey((max1, max2) => Math.max(max1, max2))
    //DStream[(Long, Long)]

    // maintain session state
    val stateBySession = maxTimeBySession.updateStateByKey((maxTimeNewValues: Seq[Long], maxTimeOldState: Option[Long]) => {
      if(maxTimeNewValues.size == 0.0){
        if(System.currentTimeMillis() - maxTimeOldState.get > SESSION_TIMEOUT_MILLIS){
          None
        }else{
          maxTimeOldState
        }
      }else if(maxTimeOldState.isEmpty){
        Some(maxTimeNewValues(0))
      }else{
        Some(Math.max(maxTimeNewValues(0), maxTimeOldState.get))
      }
    })

    val sessionCount = stateBySession.count() // DStream[Long]

    //data key types for the output map
    val SESSION_COUNT = "SESS"
    val REQ_PER_SEC = "REQ"
    val ERR_PER_SEC = "ERR"
    val ADS_PER_SEC = "AD"

    val requests = reqPerSecond.map(rq => (rq._1, Map(REQ_PER_SEC -> rq._2)))
    val errors = errorPerSecond.map(er => (er._1, Map(ERR_PER_SEC -> er._2)))
    val finalSessionCount = sessionCount.map(sc => ( (System.currentTimeMillis() / 1000) * 1000, Map(SESSION_COUNT -> sc)) )

    //maps each count to a tuple (timestamp, a Map containing the count per category under the key ADS_PER_SEC#<ad category>)
    val ads = adsPerSecondAndType.map(ads => (ads._1._1, Map(s"$ADS_PER_SEC#${ads._1._2}" -> ads._2)))

    val finalStats = finalSessionCount.union(requests).union(errors).union(ads).reduceByKey((m1, m2) => m1 ++ m2)

  }

  def ch13Realtime(ssc: StreamingContext, sparkSession: SparkSession): Unit = {


    //used for connecting to Kafka
    var brokerList: Option[String] = None
    //Spark checkpoint directory
    val checkpointDir: String = "hdfs://10.13.11.42:9000/home/freeman/spark_in_action"
    //Kafka topic for reading log messages
    var logsTopic: Option[String] = Some("weblogs")
    //Kafka topic for writing the calculated statistics
    var statsTopic: Option[String] = Some("stats")
    //Session timeout in milliseconds
    var SESSION_TIMEOUT_MILLIS = 2 * 60 * 1000 //2 minutes
    //Number of RDD partitions to use
    var numberPartitions = 3

    //-brokerList=10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094 -checkpointDir=hdfs://10.13.11.42:9000/home/freeman/spark_in_action
    /*var args: Array[String] = Array("-brokerList=10.13.11.4:9092", "-checkpointDir=hdfs://10.13.11.42:9000/home/freeman/spark_in_action")

    //this will exit if arguments are not valid
    def parseInt(str: String) = try {
      str.toInt
    } catch {
      case e: NumberFormatException => { printUsageAndExit(); 0 }
    }

    def printUsageAndExit() {
      System.err.println("Usage: StreamingLogAnalyzer -brokerList=<kafka_host1:port1,...> -checkpointDir=HDFS_DIR [options]\n" +
        "\n" +
        "Options:\n" +
        "  -inputTopic=NAME        Input Kafka topic name for reading logs data. Default is 'weblogs'.\n" +
        "  -outputTopic=NAME       Output Kafka topic name for writing aggregated statistics. Default is 'stats'.\n" +
        "  -sessionTimeout=NUM     Session timeout in minutes. Default is 2.\n" +
        "  -numberPartitions=NUM   Number of partitions for the streaming job. Default is 3.\n")
      System.exit(1)
    }
    def parseAndValidateArguments(args: Array[String]) {
      args.foreach(arg => {
        arg match {
          case bl if bl.startsWith("-brokerList=") =>
            brokerList = Some(bl.substring(12))
          case st if st.startsWith("-checkpointDir=") =>
            checkpointDir = Some(st.substring(15))
          case st if st.startsWith("-inputTopic=") =>
            logsTopic = Some(st.substring(12))
          case st if st.startsWith("-outputTopic=") =>
            statsTopic = Some(st.substring(13))
          case st if st.startsWith("-sessionTimeout=") =>
            SESSION_TIMEOUT_MILLIS = parseInt(st.substring(16)) * 60 * 1000
          case np if np.startsWith("-numberPartitions=") =>
            numberPartitions = parseInt(np.substring(18))
          case _ =>
            printUsageAndExit()
        }
      })
      if (brokerList.isEmpty || checkpointDir.isEmpty || logsTopic.isEmpty || statsTopic.isEmpty || SESSION_TIMEOUT_MILLIS < 60 * 1000 || numberPartitions < 1)
        printUsageAndExit()
    } //parseAndValidateArguments

    parseAndValidateArguments(args)*/
    //val conf = new SparkConf().setAppName("Streaming Log Analyzer")
    //val ssc = new StreamingContext(conf, Seconds(1))

    //ssc.checkpoint(checkpointDir)

    //set up the receiving Kafka stream
    //println("Starting Kafka direct stream to broker list: "+brokerList.get)
    val kafkaReceiverParams = Map[String, String](
      "metadata.broker.list" -> "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094")

    val kafkaBrokers = "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094"   // comma separated list of broker:host
    val group = "realtime"
    val kafkaParam = new mutable.HashMap[String, String]()
    kafkaParam.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    kafkaParam.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    kafkaParam.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParam.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    /* val kafkaStream = KafkaUtils.
       createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaReceiverParams, Set(logsTopic.get))*/


    val kafkaStream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](List("weblogs"), kafkaParam))

    //case class for storing the contents of each access log line
    case class LogLine(time: Long, ipAddr: String, sessId: String, url: String, method: String, respCode: Int, respTime: Int)

    val df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")

    //logStream contains parsed LogLines
    val logsStream = kafkaStream.flatMap { t => { // ConsumerRecord[String,String]

        //val fields = t._2.split(" ")
        val fields = t.value().split(" ")
        try {
          //df.parse --> Mon Aug 20 15:24:16 ICT 2018   ---> getTime() ---> TIMESTAMP
          List(LogLine(df.parse(fields(0) + " " + fields(1)).getTime(), fields(2), fields(3), fields(4), fields(5), fields(6).toInt, fields(7).toInt))
        }
        catch {
          case e: Exception => { System.err.println("Wrong line format: "+t); List() }
        }
      }
    }// DStream[LogLine]

    //CALCULATE NUMBER OF SESSIONS
    //contains session id keys its maximum (last) timestamp as value
    val maxTimeBySession = logsStream.map(r =>  (r.sessId, r.time)).reduceByKey(
      (max1, max2) => {
        Math.max(max1, max2)
      })//DStream[(String, Long)]
    /*maxTimeBySession.foreachRDD(a => {
      a.collect().foreach(b => {
        println("maxTimeBySession------" + b)
      })
    })*/
    //maxTimeBySession------(14eaf33e-f8db-4be6-9328-94dca9123901,1534753125582)
    //maxTimeBySession------(6746ef95-f4e4-4872-b824-f4762f7797e1,1534753128706)


    //update state by session id
    val stateBySession = maxTimeBySession.updateStateByKey((maxTimeNewValues: Seq[Long], maxTimeOldState: Option[Long]) => {
      println("maxTimeNewValues-----> " + maxTimeNewValues)
      println("maxTimeOldState-----> " + maxTimeOldState)
      if (maxTimeNewValues.size == 0.0){ //only old session exists

        //check if the session timed out
        if (System.currentTimeMillis() - maxTimeOldState.get > SESSION_TIMEOUT_MILLIS)
          None //session timed out so remove it from the state
        else
          maxTimeOldState //preserve the current state
      }
      else if (maxTimeOldState.isEmpty) //this is a new session; no need to check the timeout
        Some(maxTimeNewValues(0)) //create the new state using the new value (only one new value is possible because of the previous reduceByKey)
      else //both old and new events with this session id found; no need to check the timeout
        Some(Math.max(maxTimeNewValues(0), maxTimeOldState.get))
    })//updateStateByKey
    //DStream[(String, Long)] // SessionID, timeStamp

    //returns a DStream with single-element RDDs containing only the total count
    val sessionCount = stateBySession.count() // DStream[Long]
    /*sessionCount.foreachRDD(a => {
      a.collect().foreach(b => {
        println("sessionCount -------------------------------> " + b)
      })
    })*/

    //logLinesPerSecond contains (time, LogLine) tuples
    val logLinesPerSecond = logsStream.map(l => ((l.time / 1000) * 1000, l)) // DStream[(Long, LogLine)]    --> 1534566683962 ---> 1534566683000

    /*logLinesPerSecond.foreachRDD(l => {
      l.collect().foreach(a => {
        println(a._1 + " <-------------------------------------------> " + a._2)
      })
    })*/
//    1534566683000 <-------------------------------------------> LogLine(1534566683962,192.168.0.36,a1f8188d-1a37-454e-9fb2-6b9542c6125c,/,GET,200,500)
//    1534566684000 <-------------------------------------------> LogLine(1534566684411,192.168.0.63,c5e8c04e-fe47-495b-a7b4-e547b01af4ce,/,GET,200,500)
//    1534566684000 <-------------------------------------------> LogLine(1534566684821,192.168.0.213,fe484899-6afb-4d89-8425-b96719ee11ae,sia.org/ads/2/234/clickfw,GET,200,500)
//    1534566680000 <-------------------------------------------> LogLine(1534566680093,192.168.0.233,f5dea552-a871-47cc-b65d-68d619546e02,sia.org/ads/3/56/clickfw,GET,200,500)
//    1534566682000 <-------------------------------------------> LogLine(1534566682062,192.168.0.68,7f4f00c1-096f-4d60-9220-ffd97b8cee95,/,GET,200,500)


    //CALCULATE REQUESTS PER SECOND
    //this combineByKey counts all LogLines per unique second
    val reqsPerSecond = logLinesPerSecond.combineByKey[Long](
      logLine => 1L,                                               // createCombiner  --> (key, 1L)     --> (key, initialCount) --> (1534566683000, 1L)
      (c: Long, logLine: LogLine) => c + 1,                        // mergeValue      --> (key, 1L + 1) --> (key, count)        --> (1534566683000, 1L + 1)
      (combiner1: Long, combiner2: Long) => combiner1 + combiner2, // mergeCombiner(merges all the values across the partitions in the executors
                                                                        // and sends the data back to the driver) --> (key, countAcrossAllPartitions)
      new HashPartitioner(numberPartitions),                       // Partitioner
      true)                                        // MapsideCombine
    //DStream[(Long, Long)]


    //CALCULATE ERRORS PER SECOND
    val errorsPerSecond = logLinesPerSecond.
      //leaves in only the LogLines with response code starting with 4 or 5
      filter(l => {
        val respCode = l._2.respCode / 100
        respCode == 4 || respCode == 5
      }).
      //this combineByKey counts all LogLines per unique second
      combineByKey(r => 1L,
      (c: Long, r: LogLine) => c + 1,
      (c1: Long, c2: Long) => c1 + c2,
      new HashPartitioner(numberPartitions),
      true)


    //CALCULATE NUMBER OF ADS PER SECOND
    val adUrlPattern = new Regex(".*/ads/(\\d+)/\\d+/clickfw", "adtype")      // urlmatch.group will get data into first () | (\\d+)
    val adsPerSecondAndType = logLinesPerSecond.
      //filters out the LogLines whose URL's don't match the adUrlPattern.
      //LogLines that do match the adUrlPattern are mapped to tuples ((timestamp, parsed ad category), LogLine)
      flatMap(l => {
        adUrlPattern.findFirstMatchIn(l._2.url) match {
          case Some(urlmatch) => List( ((l._1, urlmatch.group("adtype")), l._2) )  // urlmatch regex.match
                                                                // List(((1534739401000,3),LogLine(1534739401678,192.168.0.204,a0296c40-f68c-4f43-a325-8feca15b0286,sia.org/ads/3/56/clickfw,GET,200,500)))
                                                                // List(((1534739404000,2),LogLine(1534739404211,192.168.0.207,b84a6085-27b5-4d92-a0db-119a61a9dc4f,sia.org/ads/2/234/clickfw,GET,200,500)))
          case None => List()
        }
      }).
      //this combineByKey counts all LogLines per timestamp and ad category
      combineByKey(r => 1.asInstanceOf[Long],
      (c: Long, r: LogLine) => c + 1,
      (c1: Long, c2: Long) => c1 + c2,
      new HashPartitioner(numberPartitions),
      true)

    /*adsPerSecondAndType.foreachRDD(a => {
      a.collect().foreach(b => {
        println("adsPerSecondAndType -------------------------------> " + b._1 + " -------- " + b._2)   // adsPerSecondAndType -------------------------------> (1534740814000,1) -------- 1
      })
    })*/


    //data key types for the output map
    val SESSION_COUNT = "SESS"
    val REQ_PER_SEC = "REQ"
    val ERR_PER_SEC = "ERR"
    val ADS_PER_SEC = "AD"

    //maps each count to a tuple (timestamp, a Map containing the count under the REQ_PER_SEC key)
    val requests = reqsPerSecond.map(sc => (sc._1,Map(REQ_PER_SEC -> sc._2)))    //DStream[(Long, Map(String -> Long))]  (1534739401000, Map("REQ" -> 2))

    //maps each count to a tuple (timestamp, a Map containing the count under the ERR_PER_SEC key)
    val errors = errorsPerSecond.map(sc => (sc._1, Map(ERR_PER_SEC -> sc._2)))  ////DStream[(Long, Map(String -> Long))]

    /*sessionCount.foreachRDD(a => {
      a.collect().foreach(b => {
        println("-----------sessionCount: " + b)
      })
    })*/
    println("sessionCount, " + sessionCount)

    //maps each count to a tuple (current time with milliseconds removed, a Map containing the count under the SESSION_COUNT key)
    val finalSessionCount = sessionCount.map(c => ((System.currentTimeMillis / 1000) * 1000, Map(SESSION_COUNT -> c)))  //DStream[(Long, Map(String -> Long))]
    //1534739525000:(SESS->10)
    //1534739555000:(SESS->10)

    //maps each count to a tuple (timestamp, a Map containing the count per category under the key ADS_PER_SEC#<ad category>)
    val ads = adsPerSecondAndType.map(stc => (stc._1._1, Map(s"$ADS_PER_SEC#${stc._1._2}" -> stc._2)))

    //-----------ads: (1534748442000,Map(AD#3 -> 3))
    //-----------ass: (1534748440000,Map(AD#1 -> 1))
    /*ads.foreachRDD(a => {
      a.collect().foreach(b => {
        println("-----------adsPerSecondAndType: " + b)
      })
    })*/


    //all the streams are unioned and combined
    val finalStats = finalSessionCount.union(requests).union(errors).union(ads).
      //and all the Maps containing particular counts are combined into one Map per timestamp.
      //This one Map contains all counts under their keys (SESSION_COUNT, REQ_PER_SEC, ERR_PER_SEC, etc.).
      reduceByKey((m1, m2) => m1 ++ m2)     // Map[String, Long] ++ Map[String,Long]



    //Each partitions uses its own Kafka producer (one per partition) to send the formatted message
    finalStats.foreachRDD(rdd => {
      rdd.foreachPartition(partition => {
        KafkaProducerWrapper.brokerList = "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094"
        val producer = KafkaProducerWrapper.instance
        partition.foreach {
          case (s, map) =>
            println("s___ " + s + "  <---> map --" + map)
            producer.send(
              "stats",
              s.toString,
              s.toString + ":(" + map.foldLeft(new Array[String](0)) { case (x, y) => { x :+ y._1 + "->" + y._2 } }.mkString(",") + ")")
        }//foreach
      })//foreachPartition
    })//foreachRDD

    println("Starting the streaming context... Kill me with ^C")

    ssc.start()
    ssc.awaitTermination()
  }

  def ch6Kafk(ssc: StreamingContext, sparkSession: SparkSession): Unit ={
    case class LicensePlates(camera_id: String, encoded_plate_image: String, encoded_vehicle_image: String, frametime: String, location: String, location_x: String, mode: String,
                             site_member_id: String, site_owner_id: String, speed: Int, timestamp: String, vehicle_plate: String){}
    //case class LicensePlatesTmp(camera_id: Long, count: Double, info: String)
    case class LicensePlatesTmp(camera_id: Long, listData: String )

    val schemaString = "camera_id encoded_plate_image encoded_vehicle_image frametime location location_x mode site_member_id site_owner_id speed timestamp vehicle_plate"
    val fields = schemaString.split(" ").map(fieldname => StructField(fieldname, StringType, nullable =true))
    val schema = StructType(fields)

    val kafkaBrokers = "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094"   // comma separated list of broker:host
    val group = "realtime"
    val topicList = List("license_plate_topic")
    val destinationTopic="elasticsearch-data"

    val mongoDbDatabase = "anpr"
    val mongoDbCollection = "license_plate"
    val MongoDbOptions = Map(MongodbConfig.Host -> "10.12.11.82:27017", MongodbConfig.Database -> mongoDbDatabase, MongodbConfig.Collection -> mongoDbCollection)

    val kafkaParam = new mutable.HashMap[String, String]()
    kafkaParam.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    kafkaParam.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParam.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    kafkaParam.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParam.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

   /* val kafkaReceiverParams = Map[String, String](
      "metadata.broker.list" -> "192.168.10.2:9092")
    val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaReceiverParams, Set("orders"))*/

    val kafkaStream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicList, kafkaParam))

    val vehiclePlates = kafkaStream.flatMap(line => {
      val s = line.value().split(",")
      try {
        List(LicensePlatesTmp(s(0).split(":")(1).replaceAll("\"", "").toLong, line.value()))//.map(f => (f.camera_id, f.listData))
      }
      catch {
        case e : Throwable => println("Wrong line format ("+e+"): "+line.value().toString)
          List()
      }
    })

    //vehiclePlates.repartition(1).saveAsTextFiles("file:///home/freeman/bigdata/data/spark_in_action/ch06/outputCh06/vehicle", "txt")

    val transformVehicle = vehiclePlates.map(vehicle => {
      val cameraID = vehicle.camera_id
      (cameraID, vehicle.listData)
    }) // DStream[(Long, String)]

    //transformVehicle.repartition(1).saveAsTextFiles("file:///home/freeman/bigdata/data/spark_in_action/ch06/outputCh06/vehicle", "txt")
    /*transformVehicle.foreachRDD(rddV => {
      rddV.collect().foreach(f => println(f._1 + "-----" + f._2))
    })*/

    val stateSpecFunc = (batchTime: Time, cameraId: Long, vehicleRecord: Option[String], state: State[List[String]]) => {
      val v = vehicleRecord.get
      if (state.exists()) {
        val currentSet = state.get() //List[String]
        if (currentSet.contains(v)) {
          None
        } else {
          state.update(currentSet.++(List(v)))
          Some(cameraId, state.get.size.toDouble)
        }
      } else {
        state.update(List(v))
        Some(cameraId, state.get.size.toDouble)
      }
    }

    //transformVehicle
    /*val updateAmountState = (batchTime: Time, cameraId: Long, count: Option[Double], state: State[Double]) => {
      var total = count.getOrElse(0.toDouble)
      if(state.exists())
        total += state.get()
      if(state.isTimingOut()){
        println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      }else{
        state.update(total)
      }
      //println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      //state.update(total)
      Some((cameraId, total))
    }*/


    val spec = StateSpec.function(stateSpecFunc) //.timeout(Durations.seconds(2000)) //StateSpec[Long, Double, Double, (Long, Double)]*/
    val amountState = transformVehicle.mapWithState(spec).stateSnapshots().map(vehiclePlate => (vehiclePlate._1, vehiclePlate._2.size, vehiclePlate._2.last))   // DStream[(Long, Double)]


    //val tmp = vehiclePlates.map(f => (f.mkString(", ")))
    //println(vehiclePlates.print())
    /*vehiclePlates.foreachRDD(vRdd => {
      vRdd.collect().foreach(f => f.mkString(", "))
    })*/

    val props = new Properties()
    props.put("bootstrap.servers", "10.13.11.4:9092,10.13.11.4:9093,10.13.11.4:9094")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")


    //val sparkDocuments = sc.parallelize((1 to 10).map(i => Document.parse(s"{spark: $i}")))
    //MongoSpark.save(sparkDocuments, writeConfig)

    //val producer = new KafkaProducer[String,String](props)
    //val sc = ssc.sparkContext

    /*val jsonSchema = new StructType()
      .add("camera_id", StringType)
      .add("encoded_plate_image", StringType)
      .add("encoded_vehicle_image", StringType)
      .add("frametime", StringType)
      .add("location", StringType)
      .add("location_x", StringType)
      .add("mode", StringType)
      .add("site_member_id", StringType)
      .add("site_owner_id", StringType)
      .add("speed", IntegerType)
      .add("timestamp", TimestampType)
      .add("vehicle_plate", StringType)*/

    def sleep(time: Long) { Thread.sleep(time) }
    amountState.foreachRDD((rdd) => {
      val f = Future {
        sleep(300)

        /*import ssc.implicits._
        val wordCounts = rdd.map({ case (word: Long, count: Double)
        => LicensePlatesTmp(String, count) }).toDF()
        wordCounts.write.mode("append").mongo()*/

        //val writeConfig = WriteConfig(Map("collection" -> "license_plate", "writeConcern.w" -> "majority", "uri" -> "mongodb://10.13.11.42:27017", "database" -> "anpr"), Some(WriteConfig(sparkSession)))
        //val documentVehicle = rdd.map(f => Document.parse(f._3))
        //MongoSpark.save(documentVehicle, writeConfig)

        //val tmpRdd = rdd.map(f => (f._3))
        //import sparkSession.implicits._
        //val dataSetRecord = sparkSession.createDataset(tmpRdd)
        //dataSetRecord.printSchema()
        //dataSetRecord.show(10)
        //val tmpSet = dataSetRecord.select("_3").toDF("camera_id","encoded_plate_image","encoded_vehicle_image", "frametime", "location" ,"location_x", "mode", "site_member_id",
        //  "site_owner_id", "speed", "timestamp", "vehicle_plate")
        //val tmpSet = dataSetRecord.select(from_json($"_3".cast(StringType), jsonSchema)).alias("value") //.select(to_json($"value").alias("value"))
        //val tmpSet = dataSetRecord.select("_3").as[String]

        //val licensePlateDf=sparkSession.read.schema(schema).json(dataSetRecord)  //DataFrame
        //licensePlateDf.printSchema()
        //licensePlateDf.show(5)
        //licensePlateDf.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()

        rdd.foreachPartition((iter) => {
          /*KafkaProducerWrapper.brokerList = "192.168.10.2:9092"
          val producer = KafkaProducerWrapper.instance
          iter.foreach({ case (metric, list) => producer.send("metrics", metric, metric + ", " + list.toString) })*/

          iter.foreach(f => {

            //import sparkSession.implicits._
            //val dataSetRecord = sparkSession.createDataset(List[])
            //println("+++++++++++++++++++++>", f._3)
            //val tmp = Seq("""{"camera_id":"22483","encoded_plate_image":"encoded_plate_img_1",
            // "location":"STMC-D985C5","location_x":"329","mode":"0","site_member_id":1111,"site_owner_id":9135,"speed":0,"timestamp":"2018-06-30T07:28:59.086Z","vehicle_plate":"49Y-63447"}""")
            // .toDS()

            //val licensePlateDf=sparkSession.read.schema(schema).json(tmp) //DataFrame
            //licensePlateDf.printSchema()
            //licensePlateDf.show(5)
            //licensePlateDf.write.format("com.mongodb.spark.sql.DefaultSource").mode("append").options(MongoDbOptions).save()

            new KafkaProducer[String,String](props).send(new ProducerRecord[String, String](destinationTopic, "myKey", f._1 + " --- " + f._2 +" --- " + f._3))
          })
          //producer.send(new ProducerRecord[String, String](destinationTopic, "myKey", tmpA))
        })
      }
      f.onComplete {
        case Success(messages) => println("yay!" + messages)
        case Failure(exception) => println("On no!" + exception)
      }
    })

    //vehiclePlates.repartition(1).saveAsTextFiles("file:///home/freeman/bigdata/data/spark_in_action/ch06/outputCh06/vehicle", "txt")
    ssc.checkpoint("hdfs://10.13.11.42:9000/home/freeman/spark_in_action")
    ssc.start()
    ssc.awaitTermination()

  }

  def ch6(ssc: StreamingContext, sc: SparkConf) = {
    //file:///home/freeman/bigdata/data/spark_in_action/ch06/input
    //hdfs://10.13.11.42:9000/home/freeman/spark_in_action
    val fileStream = ssc.textFileStream("file:///home/freeman/bigdata/data/spark_in_action/ch06/input")

    import java.sql.Timestamp
    case class Order(time: java.sql.Timestamp, orderId: Long, clientId: Long, symbol: String, amount: Int, price: Double, buy: Boolean)

    val orders = fileStream.flatMap(line => {
      val orderDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
      val s = line.split(",")

      try {
        assert(s(6) == "B" || s(6) == "S")
        List(Order(new Timestamp(orderDateFormat.parse(s(0)).getTime()), s(1).toLong, s(2).toLong, s(3), s(4).toInt, s(5).toDouble, s(6) == "B"))
      }
      catch {
        case e : Throwable => println("Wrong line format ("+e+"): "+line)
          List()
      }
    })  // DStream[Order]


   /*orders.foreachRDD(rddOrder => {
      rddOrder.collect().foreach(order => {
        println(order.time + " --- " + order.buy + " --- " + order.price)
      })
   }) */
    /*orders.foreachRDD(rddOrder => {
      println(rddOrder.count())
    })*/

    // orders | DStream[Order]
    val numPerType = orders.map(order => (order.buy, 1L)).reduceByKey((c1, c2) => c1 + c2)  // DStream[(Boolean, Long)]


    /*numPerType.foreachRDD(rdd => {
      rdd.collect().foreach(f => println(f._1 + "---" + f._2))
    })*/
    //numPerType.repartition(1).saveAsTextFiles("file:///home/freeman/bigdata/data/spark_in_action/ch06/outputCh06/numPerType", "txt")

    val amountPerClient = orders.map(order => (order.clientId, order.amount * order.price)) // DStream[(Long,Double)]

    //--------------
    // vals: la 1 Seq, nam giu gia tri moi cua key den den tu curren mini-batch (Vd key moi la ClientID-10 co gia tri (order*amount * order.price)
    // totalOpt: la state value cua key, or None neu trang thai cho key chua tung duoc tinh toan truoc do
    /*val amountState = amountPerClient.updateStateByKey((vals, totalOpt: Option[Double]) => {
      totalOpt match  {
        case Some(total) => Some(vals.sum + total)    // If state for this key already exists, sum it up with the sum of new values. //vals.sum la new value , dc lay tu Seq[]
        case None => Some(vals.sum) // Otherwise only return sum of new value
      }
    }) // DStream[(Long, Double)]*/
    //--------------


    //--------------
    /*def updateState(key: Long, value: Option[Double], state: State[Double]): Option[Double] = {
      value match {
        case Some(total) => Some(value.sum + total) // If state for this key already exists, sum it up with the sum of new values. //vals.sum la new value , dc lay tu Seq[]
        case None => Some(value.sum) // Otherwise only return sum of new value
        case _ if state.isTimingOut() => {
          println("I am here -------------------__Some(9999)")
          state.remove()
          Some(0.0)
        }// Trigger Code Here
      }
    }
    val spec = StateSpec.function(updateState _).timeout(Durations.seconds(10))
    // Use spec to invoke `mapWithState`
    val amountState = amountPerClient.mapWithState(spec).stateSnapshots()   // DStream[(Long, Double)]*/
    //----------------------

    val updateAmountState = (time: Time, clientId: Long, amount: Option[Double], state: State[Double]) => {
      var total = amount.getOrElse(0.toDouble)
      if(state.exists())
        total += state.get()
      if(state.isTimingOut()){
        println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      }else{
        state.update(total)
      }
      //println("I am here -------------------__Some(9999)", state.toString() + " _______ " + state.getOption())
      //state.update(total)
      Some((clientId, total))
    }

    /*def trackStateFunc(batchTime: Time,
                       key: String,
                       value: Option[Int],
                       state: State[Long]): Option[(String, Long)] = {
      val sum = value.getOrElse(0).toLong + state.getOption.getOrElse(0L)
      val output = (key, sum)
      if (!state.isTimingOut) state.update(sum)
      Some(output)
    }*/

    /*val updateAmountState = (time: Time, clientId: Long, amount: Option[Double], state: State[Double]) => {
      amount match {
        case Some(v) =>
          val sum = v.toLong + state.getOption.getOrElse(0.toDouble)
          state.update(sum)
          Some((clientId, sum))
        case _ if state.isTimingOut() => {
          Some(clientId, state.getOption.getOrElse(0.toDouble))
        }
      }
    }*/

      /*val updateAmountState = (time:Time, clientId:Long, amount:Option[Double], state:State[Double]) => {
        var total = amount.getOrElse(0.toDouble)
        if(state.exists())
          total += state.get()
        state.update(total)
        Some((clientId, total))
      }*/

    val spec = StateSpec.function(updateAmountState).timeout(Durations.seconds(2000)) //StateSpec[Long, Double, Double, (Long, Double)]

    val amountState = amountPerClient.mapWithState(spec).stateSnapshots()   // DStream[(Long, Double)]


    val top5Clients = amountState.transform(orderRDD => {
      orderRDD.sortBy(_._2, false).zipWithIndex().filter(_._2 < 5).map(x => x._1)
    })

    val buySellList = numPerType.map(t => {
      if(t._1) ("BUYES", List(t._2.toString))
      else ("SELLS", List(t._2.toString))
    })  // DStream[(String, List[String])]

    /*val top5clList = top5clients.repartition(1).
      map(x => x._1.toString).
      glom().
      map(arr => ("TOP5CLIENTS", arr.toList))
    */

    //top5clList ...map | DStream[String]
    // ...glom | DStream[Array[String]]
    /*Glom Return a new DStream in which each RDD is generated by applying glom() to each RDD of
    * this DStream. Applying glom() to an RDD coalesces all elements within each partition into
    * an array.
    * RDD[String] ===> RDD[Array[String]]
    * */
    val top5clList = top5Clients.repartition(1).map(x => x._1.toString).glom().map(arr => ("TOP5CLIENTS", arr.toList))  // DStream[(String, List[String])]

    //union the two DStream s together:
    val finalStream = buySellList.union(top5clList)
    finalStream.repartition(1).saveAsTextFiles("file:///home/freeman/bigdata/data/spark_in_action/ch06/outputCh06/finalStream", "txt")

    ssc.checkpoint("hdfs://10.13.11.42:9000/home/freeman/spark_in_action")


   /* // A mapping function that maintains an integer state and return a String
    def mappingFunction(key: String, value: Option[Int], state: State[Int]): Option[String] = {
      // Use state.exists(), state.get(), state.update() and state.remove()
      // to manage state, and return the necessary string
    }
    val spec = StateSpec.function(mappingFunction).numPartitions(10)
    val mapWithStateDStream = keyValueDStream.mapWithState[StateType, MappedType](spec)*/




    /*top5Clients.foreachRDD(rddOrder => {
      rddOrder.collect().foreach(f => {
        println(f._1 + " ----- " + f._2)
      })
    })*/

    ssc.start()
    ssc.awaitTermination()
    //ssc.stop(false)
  }

  def mergeData(spark: SparkSession): Unit ={
    val sc = spark.sparkContext

    case class Product(time: Int, name: String, price: Option[Int])
    case class Rate(time: Int, value: Int)

    val productPath = "file:///home/freeman/bigdata/data/mergeData/products.csv"
    val ratePath = "file:///home/freeman/bigdata/data/mergeData/rate.csv"

    /*val product = sc.textFile("file:///home/freeman/Works/bigdata/data/mergeTest/products.csv")
    //val proSplit = product.map(_.split(","))
    val proSplit = product.map(p => {
      val tmpP = p.split(",")
      List(Product(tmpP(0).toInt, tmpP(1), Some(0)))
    })

    val rate = sc.textFile("file:///home/freeman/Works/bigdata/data/mergeTest/rate.csv")
    //val rateSplit  = rate.map(_.split(","))
    val rateSplit = product.map(r => {
      val tmpR = r.split(",")
      List(Rate(tmpR(0).toInt, tmpR(1).toInt))
    })*/

    val proSchema = StructType(Array(
      StructField("p_time", StringType, true),
      StructField("name", StringType, true),
      StructField("price", StringType, false)))

    val rateSchema = StructType(Array(
      StructField("r_time", StringType, true),
      StructField("rate", StringType, true)))

    val proFile = spark.read.format("com.databricks.spark.csv")
      //.option("header", "true")
      .option("inferSchema", "true")
      .schema(proSchema)
      .load(productPath)

    val rateFile = spark.read.format("com.databricks.spark.csv")
      //.option("header", "true")
      .option("inferSchema", "true")
      .schema(rateSchema)
      .load(ratePath)

    proFile.printSchema()
    rateFile.printSchema()

    proFile.show(10, false)
    rateFile.show(10, true)

    import spark.implicits._
    val files_joinDF = proFile.join(broadcast(rateFile), proFile("p_time").equalTo(rateFile("r_time")), "inner")
      .selectExpr("p_time", "name", "price", "r_time", "rate")
      .withColumn("new_price", ($"rate") * ($"price"))

    files_joinDF.printSchema()
    files_joinDF.show(10, false)

    //val files_joinDF = proFile.join(rateFile, proFile("p_time").equalTo(rateFile("r_time")), "inner")
    files_joinDF.write.format("json").save("file:///home/freeman/bigdata/data/mergeData/output")
  }

  case class Post (commentCount:Option[Int], lastActivityDate:Option[java.sql.Timestamp],
                   ownerUserId:Option[Long], body:String, score:Option[Int], creationDate:Option[java.sql.Timestamp],
                   viewCount:Option[Int], title:String, tags:String, answerCount:Option[Int],
                   acceptedAnswerId:Option[Long], postTypeId:Option[Long], id:Long)

  def ch5(spark: SparkSession, sc: SparkContext): Unit ={

    val itPostsRows = sc.textFile("file:///home/freeman/bigdata/data/spark_in_action/ch05/italianPosts.csv")   // RDD[String]



    //commentCount —Number of comments related to the question/answer
    //lastActivityDate —Date and time of the last modification
    //ownerUserId —User ID of the owner
    //body —Textual contents of the question/answer
    //score —Total score based on upvotes and downvotes
    //creationDate —Date and time of creation
    //viewCount —View count
    //title —Title of the question
    //tags —Set of tags the question has been marked with
    //answerCount —Number of related answers
    //acceptedAnswerId —If a question contains the ID of its accepted answer
    //postTypeId —Type of t
    //id —Post’s unique ID

    /*case class ItPost(commentCount: Int, lastActivityDate: Long, ownerUserId: Int, body: String, score: Int, creationDate: Long, viewCount: Int,
                      title: String, tags: String, answerCount: Int, acceptedAnswerId: Int, postTypeId: Int, id: Int)
    import spark.implicits._
    val itPostsRDD = itPostsRows.flatMap(post => {
      val x = post.split("~")
      List(ItPost(x(0).toInt,x(1).toLong,x(2).toInt,x(3),x(4).toInt,x(5).toLong,x(6).toInt,x(7),x(8),x(9).toInt,x(10).toInt,x(11).toInt,x(12).toInt))
    })
    itPostsRDD.show(10)*/

    //------------ CREATING A DATAFRAME FROM AN RDD OF TUPLES
    val itPostsSplit = itPostsRows.map(_.split("~"))            // RDD[Array[String]]

    import spark.implicits._
    val itPostsRDD = itPostsSplit.map(x => (x(0),x(1),x(2),x(3),x(4),x(5),x(6),x(7),x(8),x(9),x(10),x(11),x(12))) // RDD [(String, String...)]
    //val itPostsDFrame = itPostsRDD.toDF()
    //itPostsDFrame.show(10)
    val itPostsDF = itPostsRDD.toDF("commentCount", "lastActivityDate", "ownerUserId", "body", "score", "creationDate", "viewCount", "title", "tags", "answerCount", "acceptedAnswerId", "postTypeId", "id")
    itPostsDF.printSchema

    //------------CONVERTING RDDS TO DATAFRAMES USING CASE CLASSES
    import java.sql.Timestamp
    object StringImplicits {
      implicit class StringImprovements(val s: String) {
        import scala.util.control.Exception.catching
        def toIntSafe = catching(classOf[NumberFormatException]) opt s.toInt
        def toLongSafe = catching(classOf[NumberFormatException]) opt s.toLong
        def toTimestampSafe = catching(classOf[IllegalArgumentException]) opt Timestamp.valueOf(s)
      }
    }

    import StringImplicits._
    def stringToPost(row: String): Post = {
      val r = row.split("~")
      Post(r(0).toIntSafe, r(1).toTimestampSafe, r(2).toLongSafe, r(3), r(4).toIntSafe, r(5).toTimestampSafe, r(6).toIntSafe, r(7), r(8), r(9).toIntSafe,
        r(10).toLongSafe, r(11).toLongSafe, r(12).toLong)
    }
    val itPostsDFCase = itPostsRows.map(x => stringToPost(x)).toDF()
    //itPostsDFCase.printSchema()

    //-------------CONVERTING RDDS TO DATAFRAMES BY SPECIFYING A SCHEMA
    import org.apache.spark.sql.types._

    val postSchema = StructType(Seq(
      StructField("commentCount", IntegerType, true),
      StructField("lastActivityDate", TimestampType, true),
      StructField("ownerUserId", LongType, true),
      StructField("body", StringType, true),
      StructField("score", IntegerType, true),
      StructField("creationDate", TimestampType, true),
      StructField("viewCount", IntegerType, true),
      StructField("title", StringType, true),
      StructField("tags", StringType, true),
      StructField("answerCount", IntegerType, true),
      StructField("acceptedAnswerId", LongType, true),
      StructField("postTypeId", LongType, true),
      StructField("id", LongType, false)
    ))//StructType


    def stringToRow(row:String): Row = {
      val r = row.split("~")

      Row(r(0).toIntSafe.getOrElse(null),
        r(1).toTimestampSafe.getOrElse(null),
        r(2).toLongSafe.getOrElse(null),
        r(3),
        r(4).toIntSafe.getOrElse(null),
        r(5).toTimestampSafe.getOrElse(null),
        r(6).toIntSafe.getOrElse(null),
        r(7),
        r(8),
        r(9).toIntSafe.getOrElse(null),
        r(10).toLongSafe.getOrElse(null),
        r(11).toLongSafe.getOrElse(null),
        r(12).toLong)
    }

    val rowRDD = itPostsRows.map(stringData => stringToRow(stringData))
    val itPostsDFStruct = spark.createDataFrame(rowRDD, postSchema)
    //println(itPostsDFStruct.columns)
    //println(itPostsDFStruct.dtypes)

    val postsDf = itPostsDFStruct
    postsDf.printSchema()
    postsDf.show(10)
    //val postsIdBody = postsDf.select("id", "body")
    //val postsIdBody = postsDf.select(postsDf.col("id"), postsDf.col("body"))
    //val postsIdBody = postsDf.select(Symbol("id"), Symbol("body"))
    //val postsIdBody = postsDf.select('id, 'body)
    val postsIdBody = postsDf.select($"id", $"body")

    //val postIds = postsIdBody.drop("body")
    val countPostItaliano = postsIdBody.filter('body contains "Italiano").count()
    println("-----countPostItaliano: " + countPostItaliano)
    postsIdBody.show(10)

    val noAnswer = postsDf.filter(('postTypeId === 1) and ('acceptedAnswerId isNull))
    //noAnswer.printSchema()
    //noAnswer.show(1, false)

    postsDf.filter('postTypeId === 1).withColumn("ratio", 'viewCount / 'score).where('ratio < 35).show()

    //The 10 most recently modified questions:
    postsDf.filter('postTypeId === 1).orderBy('lastActivityDate desc).limit(10).show


    import org.apache.spark.sql.functions._
    val postActivePeriod = postsDf.filter('postTypeId === 1)
      .withColumn("activePeriod", datediff('lastActivityDate, 'creationDate))
      .orderBy('activePeriod desc)
      .head.getString(3).replace("&lt;","<").replace("&gt;",">")

    //println("postActivePeriod--------------" + postActivePeriod)

    //postsDf.select(avg('score), max('score), count('score)).show

    postsDf.filter('postTypeId === 1)
        .select('ownerUserId, 'acceptedAnswerId, 'score, max('score).over(Window.partitionBy('ownerUserId)) as "maxPerUser")
        .withColumn("toMax", 'maxPerUser - 'score)//.show(10)

    postsDf.filter('postTypeId === 1)
      .select('ownerUserId, 'id, 'creationDate,
        lag('id, 1).over(Window.partitionBy('ownerUserId).orderBy('creationDate)) as "prev",
        lead('id, 1).over(Window.partitionBy('ownerUserId).orderBy('creationDate)) as "next")
      .orderBy('ownerUserId, 'id)//.show(50, false)

    //val countTags = udf((tags: String) => "&lt;".r.findAllMatchIn(tags).length)
    val countTags = spark.udf.register("countTags", (tags: String) => "&lt;".r.findAllMatchIn(tags).length)
    postsDf.filter('postTypeId === 1).select('tags, countTags('tags) as "tagCnt")//.show(10, false)

    //Section 5.1.4
    //val cleanPosts = postsDf.na.drop()
    //println("DF using na drop: " + cleanPosts.count())

    //postsDf.na.fill(Map("viewCount" -> 0))

    //val postsDfCorrected = postsDf.na.replace(Array("id", "acceptedAnswerId"), Map(1177 -> 3000)).show(5)

    //-----
    // Row.fromSeq()  -- // Create a Row from a Seq of values.
    val postsMapped = postsDf.rdd.map(row => Row.fromSeq(
      row.toSeq
        .updated(3, row.getString(3).replace("&lt;", "<").replace("&gt;", ">"))
        .updated(8, row.getString(8).replace("&lt;", "<").replace("&gt;", ">"))
    ))

    val postsDfNew = spark.createDataFrame(postsMapped, postsDf.schema)


    postsDfNew.groupBy('ownerUserId, 'tags,
      'postTypeId).count.orderBy('ownerUserId desc).show(5)

    postsDfNew.groupBy('ownerUserId).agg(last('lastActivityDate) as('lastActivityDate), max('score)).filter(max('score) > 5).orderBy('ownerUserId).show(5, false)
    //postsDfNew.groupBy('ownerUserId).agg(Map("lastActivityDate" -> "max", "score" -> "max")).orderBy('ownerUserId).show(10, false)

    postsDfNew.groupBy('ownerUserId).agg(last('lastActivityDate) as('lastActivityDate), max('score).gt(5)).orderBy('lastActivityDate desc).show(5, false)

    val smplDf = postsDfNew.where('ownerUserId >= 13 and 'ownerUserId <= 15)//.show(50)
    //smplDf.groupBy('ownerUserId, 'tags, 'postTypeId).count.orderBy('ownerUserId).show()
    //smplDf.orderBy('ownerUserId).show(5)
    //smplDf.rollup('ownerUserId, 'tags, 'postTypeId).count.orderBy('ownerUserId).show()
    smplDf.rollup('ownerUserId, 'tags, 'postTypeId).count.sort($"ownerUserId".desc_nulls_last, $"tags".asc_nulls_last, $"postTypeId".asc_nulls_last).show(5)


    //--------------------
    val itVotesRaw = sc.textFile("file:///home/freeman/bigdata/data/spark_in_action/ch05/italianVotes.csv")
      .map(x => x.split("~"))


    /*itVotesRaw.foreach(f => {
      f.foreach(a => {
        println("-------------a: " + a)
      })
    })*/

    //2657~135~2~2013-11-22 00:00:00.0
    val itVotesRows = itVotesRaw.map(row => Row(row(0).toLongSafe, row(1).toLongSafe, row(2).toIntSafe, Timestamp.valueOf(row(3))))
    val votesSchema = StructType(Seq(
      StructField("id", LongType, false),
      StructField("postId", LongType, false),
      StructField("voteTypeId", IntegerType, false),
      StructField("creationDate", TimestampType, false)) )
    val votesDf = spark.createDataFrame(itVotesRows, votesSchema).show(10)
    //val postsVotes = postsDf.join(votesDf, postsDf("id") === 'postId)
    //val postsVotes = postsDf.join(broadcast(votesDf), postsDf("id").equalTo(votesDf("postId")), "inner")

    //val postsVotesOuter = postsDf.join(votesDf, postsDf("id") === 'postId, "outer")
    //postsVotes.show(10)

    /*val files_joinDF = proFile.join(broadcast(rateFile), proFile("p_time").equalTo(rateFile("r_time")), "inner")
      .selectExpr("p_time", "name", "price", "r_time", "rate")
      .withColumn("new_price", ($"rate") * ($"price"))*/
  }

  def ch4(spark: SparkSession) = {

    val sc = spark.sparkContext
    val tranFile = sc.textFile("file:///home/freeman/bigdata/data/spark_in_action/ch04/ch04_data_transactions.txt") // RDD[String]
    val tranData = tranFile.map(_.split("#")) // RDD[Array[String]]

    /** Send a bear doll to the customer who made the most transactions **/
    val transByCust = tranData.map(tran => (tran(2).toInt, tran)) // RDD[(Int, Array[String])]
    transByCust.keys.distinct().count()
    println("Keys_Distinct_Count", transByCust.keys.distinct().count())
    println("Transaction By Key", transByCust.countByKey())

    // transByCust.countByKey()  // Map[Int, Long]
    // transByCust.countByKey().toSeq  // Seq[Int, Long]
    //transByCust.countByKey().toSeq.sortBy
    val (cid: Int, purch: Long) = transByCust.countByKey().toSeq.sortBy(_._2).last // (Int, Long)
    print((cid, purch))
    var complTrans = Array(Array("2015-03-30", "11:59 PM", "53", "4", "1", "0.00"))

    //print information by cid (53)
    val victim = transByCust.filter(_._1 == cid) // RDD[(Int, Array[String])]
    victim.collect().foreach(transaction => {
      println("cID: " + transaction._1 + "__Information: " + transaction._2.mkString(", "))
    })
    transByCust.lookup(cid).foreach( transaction => {
      println(transaction.mkString(", "))
    })


    /** Give a 5% discount for two or more Barbie Shopping Mall Playsets bought **/
    val newTransByCust = transByCust.mapValues(transaction => {
      if(transaction(3).toInt == 25 && transaction(4).toDouble > 1){
        transaction(5) = (transaction(5).toDouble * 0.95).toString
      }
      transaction
    })

    /** Add a toothbrush for more than five dictionaries bought **/
    val transByCustAddToothbrush = newTransByCust.flatMapValues(tran => {
      if(tran(3).toInt == 81 && tran(4).toDouble >= 5){
        val cloned = tran.clone()
        cloned(5) = "0.00"
        cloned(3) = "70"
        cloned(4) = "1"
        List(tran, cloned)
      }else{
        List(tran)
      }
    })
    //println("%_______________transByCustAddToothbrush.count()", transByCustAddToothbrush.count())
    /*transByCustAddToothbrush.collect().foreach(transaction => {
      println(transaction._1 + " <<>> " + transaction._2.mkString(", "))
    })*/

    //println(resultRDD.collect.foreach(f => println(f)))

    val amounts = transByCustAddToothbrush.mapValues(t => t(5).toDouble)
    /*amounts.collect().foreach(f => {
      println(f._1 + "___" + f._2)
    })*/
    val totals = amounts.foldByKey(0)((p1, p2) => p1 + p2).collect()
    amounts.collect().foreach(f => {
      println(f._1 + "___" + f._2)
    })
    println("Largest Element", totals.toSeq.sortBy(_._2).last)

    // Array(Array(2015-03-30, 11:59 PM, 53, 4, 1, 0.00), Array(2015-03-30, 11:59 PM, 76, 63, 1, 0.00))
    complTrans = complTrans :+ Array("2015-03-30", "11:59 PM", "76", "63", "1", "0.00")

    // Add transactions array to Transaction RDD
    var transByCustComplete = transByCustAddToothbrush.union(sc.parallelize(complTrans).map(t => (t(2).toInt, t)))
    //transByCustComplete.map(t => t._2.mkString("#")).saveAsTextFile("file:///home/freeman/bigdata/data/spark_in_action/ch04/ch04output-transByCust")

    val prods = transByCustComplete.aggregateByKey(List[String]())(
      (prods, tran) => prods ::: List(tran(3)),
      (prods1, prods2) => prods1 ::: prods2)

    transByCustComplete.aggregateByKey(List[String]())(
      (prods0, tran) => prods0 ::: List(tran(3)),
      (prods1, prods2) => prods1 ::: prods2)

    prods.collect()
  }


  def ch3(spark: SparkSession) = {

    val sc = spark.sparkContext
    val homeDir = System.getenv("HOME")
    println("@_HomeDir" + homeDir)  // /home/freeman
    val inputPath = "file:///home/freeman/bigdata/data/spark_in_action/ch03/*.json"
    val ghLog = spark.read.json(inputPath) //DataFrame
    //ghLog.printSchema()
    val pushes = ghLog.filter("type = 'PushEvent'") // Dataset[Row]
    //pushes.printSchema
    println("__________#___All events: " + ghLog.count)
    println("__________#___Only pushes: " + pushes.count)
    //pushes.show(20)
    val grouped = pushes.groupBy("actor.login").count() // DataFrame
    //grouped.show(20, false)
    //grouped("count").desc // Column COUNT desc
    val ordered = grouped.orderBy(grouped("count").desc) // Dataset[Row]
    //ordered.show(20)

    // Broadcast the employees set
    val empPath = "/home/freeman/bigdata/data/spark_in_action/ch03/ghEmployees.txt"
    // fromFile(empPath) <--- BufferedSource
    // BufferedSource.getLine <---- Iterator[String
    //val tmp = fromFile(empPath).getLines()  // Iterator[String]

    val employees = Set() ++ (
      for {
        line <- fromFile(empPath).getLines()
      } yield line.trim)    // line | String

    //yield also operates on every cycle of the for loop, adding a value
    //to a hidden collection that will be returned (and destroyed) as
    //the result of the entire for expression, once the loop ends.

    println("#___________@: Employees", employees) // Set[String]

    val bcEmployees = sc.broadcast(employees) // Broadcast[Set[String]]
    //println("#___________@: Broadcast[Set[String]]_________", bcEmployees.value) // Set[String]

    import spark.implicits._
    // Anonymous Function by using =>
    // (user: String) : parameter with String Type
    // (String => Boolean) truyen vao String, tra ve boolean
    val isEmp: (String => Boolean) = (user: String) => bcEmployees.value.contains(user) // bcEmployees.value | Set[String] ---> contain(user) | Boolean

    val sqlFunc = spark.udf.register("SetContainsUdf", isEmp)   // UserDefinedFunction

    val filtered = ordered.filter(sqlFunc($"login")) // Dataset[Row]
    filtered.show()
    filtered.write.format("json").save("file:///home/freeman/bigdata/data/spark_in_action/ch03/emp-gh-push-output")



    //val tmpUDF = spark.udf.register("UpperCaseLogin", (name: String) => name == name.toUpperCase)
    //ordered.filter(tmpUDF($"login")).show()
  }
}
