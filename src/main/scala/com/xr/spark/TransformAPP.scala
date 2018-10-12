package com.xr.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * 黑名单过滤
  */
object TransformAPP {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("StateFulWordCount").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    /**
      * 构建黑名单
      */
    val blacks = List("zs", "ls")
    val blacksRDD = ssc.sparkContext.parallelize(blacks).map(x => (x, true))
    val lines = ssc.socketTextStream("local", 6789)
    val clicklog = lines.map(x => (x.split(",")(1), x)).transform(rdd => {
      rdd.leftOuterJoin(blacksRDD)
        .filter(x => x._2._2.getOrElse(false) != true)
        .map(x => x._2._1)
    })
    clicklog.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
/**
  * 黑名单过滤
  *-------------------------------------------------
  * 访问日志  ==> DStream
  * 20180808,zs
  * 20180808,ls
  * 20180808,ww
  *   ====> (zs:20180808) (ls:20180808) (ww:20180808)
  *-------------------------------------------------
  * 黑名单列表 ===> RDD
  * zs
  * ls
  *   ===>(zs:true) (ls:true)
  *---------------------------------------------------
  *
  *======> 20180808,ww
  *----------------------------------------------------
  *leftjoin
  * (zs:[<20180808,zs>,<true>])
  * (ls:[<20180808,ls>,<true>])
  * (ww:[<20180808,ww>,<false>])    ====>tuple 1
  **/

