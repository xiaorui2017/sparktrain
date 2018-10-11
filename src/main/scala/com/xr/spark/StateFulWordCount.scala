package com.xr.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object StateFulWordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("StateFulWordCount").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    //如果使用了stateful算子，必须要设置checkpoint
    //在生产环境中，建议大家把checkpoint设置到HDFS的某个文件夹中
    ssc.checkpoint(".")
    val lines = ssc.socketTextStream("local", 6789)
    val result = lines.flatMap(_.split(" ")).map((_, 1))
    val state = result.updateStateByKey[Int](updateFunction _)
    state.print()
    ssc.start()
    ssc.awaitTermination()
  }
/** 
* @@Description:  把当前的数据去更新已有的或者是老的数据
* @@Param: [currentValues, preValues]  当前的  老的
* @@return: scala.Option<java.lang.Object>
* @@Author: XR
* @@Date: 2018/10/11
* @@Time: 12:20
*/
  def updateFunction(currentValues: Seq[Int], preValues: Option[Int]): Option[Int] = {
    val current = currentValues.sum
    val pre = preValues.getOrElse(0)
    Some(current + pre)
  }

}
