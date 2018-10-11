package com.xr.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * 使用Spark Streaming处理文件系统(local/hdfs)的数据
  **/
object FileWordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("FileWordCount").setMaster("local[2]")

    val ssc = new StreamingContext(sparkConf, Seconds(5))
    val lines = ssc.textFileStream("file://Users/root/ss")
    val result = lines.flatMap(_.split(" ")).map((_, 1)).reduceByKey(_ + _)
    result.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
