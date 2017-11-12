package com.playground.streaming

import java.util
import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord
}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{
  DataFrame,
  Dataset,
  SQLContext,
  SaveMode,
  SparkSession
}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

object DigestHDFS {

  def main(args: Array[String]): Unit = {

    val config: Config = ConfigFactory.load()
    val digestConfig: Config = config.getConfig("digesthdfs")
    val sparkConfig: Config = digestConfig.getConfig("spark")
    val kafkaConfig: Config = digestConfig.getConfig("kafka")
    val hdfsConfig: Config = digestConfig.getConfig("hdfs")

    val conf = new SparkConf()
      .setAppName(sparkConfig.getString("app.name"))

    val sparkSession: SparkSession =
      SparkSession.builder().config(conf).getOrCreate()
    val ssc =
      new StreamingContext(sparkSession.sparkContext, Seconds(1))

    import sparkSession.implicits._

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafkaConfig.getString("bootstrap.servers"),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> UUID.randomUUID().toString,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val kafkaTopic = kafkaConfig.getString("input.topic")
    val kafkaBrokers = kafkaConfig.getString("bootstrap.servers")
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](Array(kafkaTopic), kafkaParams)
    )

    val props = new util.HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
              "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
              "org.apache.kafka.common.serialization.StringSerializer")

    stream.foreachRDD { rdd =>
      rdd
        .map(_.value)
        .toDF()
        .write
        .mode(SaveMode.Append)
        .json(hdfsConfig.getString("output.dir"))
    }

    ssc.start()
    ssc.awaitTermination()
    ssc.stop(stopSparkContext = true, stopGracefully = true)
  }

}
