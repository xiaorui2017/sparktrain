package com.xr.spark

import java.sql.DriverManager

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * 使用Spark Streaming完成有状态统计，并将结果写入到MySQL中
  */
object ForeachRDDApp {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("StateFulWordCount").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    //如果使用了stateful算子，必须要设置checkpoint
    //在生产环境中，建议大家把checkpoint设置到HDFS的某个文件夹中
    ssc.checkpoint(".")
    val lines = ssc.socketTextStream("local", 6789)
    val result = lines.flatMap(_.split(" ")).map((_, 1)).reduceByKey(_ + _)

    //result.print() //此处仅仅将结果输出到控制台

    // TODO... 将结果写入到MYSQL
    //    result.foreachRDD(rdd=>{
    //      val connection=createConnection() //executed at the driver
    //      rdd.foreach{
    //        record=>
    //          val sql ="insert into wordcount(word,wordcount) values('"+record._1+"',"+record._2+")"
    //          connection.createStatement().execute(sql)
    //      }
    //    })

    result.foreachRDD(rdd => {
      rdd.foreachPartition(partitionOfRecords => {
        if (partitionOfRecords.size > 0) {
          val connection = createConnection() //executed at the driver
          partitionOfRecords.foreach(record => {
            val sql = "insert into wordcount(word,wordcount) values('" + record._1 + "'," + record._2 + ")"
            connection.createStatement().execute(sql)
          })
          connection.close()
        }
      })
    })

    ssc.start()
    ssc.awaitTermination()
  }

  /**
    * @@Description: 获取MySQL的链接
    * @@Param: []
    * @@return: java.sql.Connection
    * @@Author: XR
    * @@Date: 2018/10/11
    * @@Time: 12:46
    */
  def createConnection() = {
    Class.forName("com.mysql.jdbc.Driver")
    DriverManager.getConnection("jdbc:mysql://localhost:3306/Test_spark", "root", "123456")
  }
}
/**
  *需求：将结果写入到MySQL
  * create table wordcount(
  * word varchar(50) default null,
  * wordcount int(10) default null
  * );
  *
  *存在的问题：
  * 1）对于已有的数据做更新，而是所有的数据均为insert
  * 改进思路：
  *   a) 在插入数据前做判断单词是否存在，如果存在就update不存在则insert
  *   b) 在工作中：HBASE/Redis等nosql数据库
  *
  * 2）每个rdd的partition创建connection，建议大家改成连接池
  *
*/