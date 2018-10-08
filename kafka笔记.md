# kafka内容介绍



## Kafka的应用

### 理解 ApacheKafka是什么

​	Apache Kafka 企业级消息队列    ---->是什么？有什么用？怎么用？

是什么？三个定义

- Apache Kafka 是一个消息队列（生产者消费者模式）
- Apache Kafka 目标：构建企业中统一的、高通量、低延时的消息平台。
- 大多的是消息队列（消息中间件）都是基于JMS标准实现的，Apache Kafka 类似于JMS的实现。

有什么用？（消息队列有什么用？）

- 作为缓冲，来异构、解耦系统。

  ![](../img/AMQ和kafka的区别.png)

### 掌握ApacheKafka的基本架构

- Kafka Cluster：由多个服务器组成。每个服务器单独的名字broker。
- Kafka Producer：生产者、负责生产数据。
- Kafka consumer：消费者、负责消费数据。
- Kafka Topic: 主题，一类消息的名称。存储数据时将一类数据存放在某个topci下，消费数据也是消费一类数据。
  - 订单系统：创建一个topic，叫做order。
  - 用户系统：创建一个topic，叫做user。
  - 商品系统：创建一个topic，叫做product。


- 注意：Kafka的元数据都是存放在zookeeper中。

  ![](../img/kafka架构.png)

### 搭建Kafka集群

1. 准备环境
   1. 安装jdk,安装zookeeper
   2. 安装目录规范
2. 启动zookeeper集群
   1. 一键启动脚本--->需要自己编写
   2. 手动启动
3. 部署kafka集群
   1. 安装包的下载,解压,重命名
   2. 修改配置文件
      1. server.properties  
         1. broker.id

         2. log.dirs

         3. zookeeper.connect

   3. 分发节点
4. 使用工具查看kafka集群
   1. zooinspector

### 掌握操作集群的两种方式

1. 使用控制台操作集群

   创建一个topic

   ```shell
   ./kafka-topics.sh  --create --partitions 3 --replication-factor 2 --topic order --zookeeper node01:2181,node02:2181,node03:2181
   ```

   生产者生产消息

   ```shell
   ./kafka-console-producer.sh  --broker-list node01:9092,node02:9092,node03:9092 --topic order
   ```

   消费者消费消息

   ```shell
   ./kafka-console-consumer.sh  --bootstrap-server node01:9092,node02:9092,node03:9092 --from-beginning --topic order
   ```

2. 使用java API操作集群

   ​	1.1创建maven工程,导入kafka-clients依赖

   ```java
   <dependency>
       <groupId>org.apache.kafka</groupId>
       <artifactId>kafka-clients</artifactId>
       <version>0.11.0.1</version>
   </dependency>
   ```

   ​	1.2生产订单消息

   ```java
   import org.apache.kafka.clients.producer.KafkaProducer;
   import org.apache.kafka.clients.producer.ProducerRecord;

   import java.util.Properties;

   /**
    * 订单的生产者代码
    */
   public class OrderProducer {
       public static void main(String[] args) throws InterruptedException {
           /* 1、连接集群，通过配置文件的方式
            * 2、发送数据-topic:order，value
            */
           Properties props = new Properties();
           props.put("bootstrap.servers", "node01:9092");
           props.put("acks", "all");
           props.put("retries", 0);
           props.put("batch.size", 16384);
           props.put("linger.ms", 1);
           props.put("buffer.memory", 33554432);
           props.put("key.serializer",
                   "org.apache.kafka.common.serialization.StringSerializer");
           props.put("value.serializer",
                   "org.apache.kafka.common.serialization.StringSerializer");
           KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(props);

           for (int i = 0; i < 10000; i++) {
               // 发送数据 ,需要一个producerRecord对象,最少参数 String topic, V value
               ProducerRecord<String, String> partition = new ProducerRecord<String, String>("order", 0, "key", "订单"+i);
               ProducerRecord<String, String> key = new ProducerRecord<String, String>("order", "key", "value"+i);
               ProducerRecord<String, String> value = new ProducerRecord<String, String>("order", "订单信息！"+i);
               kafkaProducer.send(value);
               /**
                * 如果ProducerRecord中制定了数据发送那个partition，就用这个编号
                * 平常一般不指定partition
                */
           }
       }
   }
   ```

   ​	2.消费订单消息

   ```java
   import org.apache.kafka.clients.consumer.ConsumerRecord;
   import org.apache.kafka.clients.consumer.ConsumerRecords;
   import org.apache.kafka.clients.consumer.KafkaConsumer;

   import java.util.Arrays;
   import java.util.Properties;

   /**
    * 消费订单数据--- javaben.tojson
    */
   public class OrderConsumer {
       public static void main(String[] args) {
           // 1、连接集群
           Properties props = new Properties();
           props.put("bootstrap.servers", "node01:9092");
           props.put("group.id", "test");
           props.put("enable.auto.commit", "true");
           props.put("auto.commit.interval.ms", "1000");
           props.put("key.deserializer",
                   "org.apache.kafka.common.serialization.StringDeserializer");
           props.put("value.deserializer",
                   "org.apache.kafka.common.serialization.StringDeserializer");
           KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(props);
           // 2、发送数据 发送数据需要，订阅下要消费的topic。  order
           kafkaConsumer.subscribe(Arrays.asList("order"));
           while (true) {
               ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(100);// jdk queue offer插入、poll获取元素。 blockingqueue put插入原生，take获取元素
               for (ConsumerRecord<String, String> record : consumerRecords) {
                   System.out.println("消费的数据为：" + record.value());
               }
           }
       }
   }
   ```



## Kafka基本原理

### 熟悉ApacheKafka原理

1. Apache Kafka原理-分片与副本机制

   **分片机制**:针对一个topic中,当数据量非常大的时候，一个服务器存放不了，就将数据分成两个或者多个部分，存放在多台服务器上。每个服务器上的数据，叫做一个partition

   **副本机制**:当数据只保存一份的时候，有丢失的风险。为了更好的容错和容灾，将数据拷贝几份，保存到不同的机器上。

2. Apache Kafka原理-消息存储及查询机制

   1. **文件的存储机制**:每个partition中会存在多个segment段文件,默认每个segment文件大小是1G,segment是由两个核心文件.index和.log组成。.index中存放文件的索引,.log中存放数据信息

   2. segment段中有两个核心的文件**一个是log,存放真实数据** ; **一个是index,存放索引文件**。

   3. 演示:当log文件等于1G时，新的会写入到下一个segment中,一个segment段差不多会存储70万条数据。

   4. 命名规则与查询机制

      文件查询时,通过index文件中确定数据索引,每个索引和数据一一对应,在log文件中读取到数据。

3. Apache Kafka原理-生产者数据分发策略

   1. 说明:

      1. 当在构造参数中指定partition时,数据发往指定分区
      2. 当在构造参数中没有指定partition时,但是数据有key时,根据key的hash值分区
      3. 在使用构造参数中,既没有指定partition,也没有key时,采用轮询调度

   2. 代码演示:

      三种机制

4. Apache Kafka原理-消费者的负载均衡机制

   一个partition只能被消费组中的一个成员消费。
   所以如果消费组中有多于partition数量的消费者，那么一定会有消费者无法消费数据,处于空闲状态

5. Apache Kafka原理-消息不丢失机制

   1. 生产者端消息不丢失机制:消息生产分为同步模式和异步模式

      ack确认有三个状态:0表示只管发送数据,不确认是否发送成功。

      ​				1表示生产端发送数据会确认partition备份leader是否收到数据。

      ​				-1表示会确认partition所有备份节点是否都收到数据。                            

      2.1、broker端的数据不丢失机制采用备份机制

      2.2、消费者端的数不丢失机制:

      在kafka0.8 版本以前,将消息的消费情况保存在zookeeper上,记录offset值,不会丢失消息,但可能会有重复消费。0.8版本以后,消息的消费可以保存在kafka内置topic:consumer_offset上


核心概念

一键启动脚本

## 了解Apache Kafka 监控及运维

​	Zooinspector工具

​	kafka manage(扩展)
