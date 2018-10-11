package com.xr.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Spark Streaming 处理socket数据
  * 测试 ： nc
  **/
object NetworkWordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
    /**
      * 创建StreamingContext 需要两个参数：SparkConf和batch interval
      **/
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val lines = ssc.socketTextStream("localhost", 6789)

    val result = lines.flatMap(_.split(" ")).map((_, 1)).reduceByKey(_ + _)

    result.print()

  }

}
