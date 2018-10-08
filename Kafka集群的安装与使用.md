# Kafka集群的安装与使用

## 1、集群安装

* 下载安装包


* 根据scala的版本提供了两个下载包，下载对应Scala2.11的版本。


* 解压

  * tar -zxvf xxx.tgz

* 修改配置文件

  * 查看原生的配置文件有哪些

    * cat server.properties |grep -v "#"

  * 得到默认的配置文件，之后进行三个地方的修改

    ```shell
    # broker代表我们kafka集群中的每个broker实例。这个是唯一的。
    broker.id=0
    num.network.threads=3
    num.io.threads=8
    socket.send.buffer.bytes=102400
    socket.receive.buffer.bytes=102400
    socket.request.max.bytes=104857600

    # 日志目录存放的地方 mkdir -p /export/logs/kafka .在每台机器上都创建好。
    log.dirs=/export/logs/kafka
    num.partitions=1
    num.recovery.threads.per.data.dir=1
    offsets.topic.replication.factor=1
    transaction.state.log.replication.factor=1
    transaction.state.log.min.isr=1
    log.retention.hours=168
    log.segment.bytes=1073741824
    log.retention.check.interval.ms=300000

    # 由于存放了很多元数据信息到zookeeper中，需要制定zookeeper地址 zk01是一个hostname，必须在hosts文件中进行映射。
    zookeeper.connect=zk01:2181,zk02:2181,zk03:2181
    zookeeper.connection.timeout.ms=6000
    group.initial.rebalance.delay.ms=0
    ```

  * 分发安装信息

    * scp -r kafka/ node02:$PWD
    * scp -r kafka/ node03:$PWD

  * 修改brokerid

    * 在node02上将 broker.id=0 修改为 broker.id=1
    * 在node03上将 broker.id=0 修改为 broker.id=2

* 启动kafka集群，启动三个broker

  * node01

    ```shell
    nohup /export/servers/kafka/bin/kafka-server-start.sh /export/servers/kafka/config/server.properties >/dev/null 2>&1 &
    ```

  * node02

    ```
    nohup /export/servers/kafka/bin/kafka-server-start.sh /export/servers/kafka/config/server.properties >/dev/null 2>&1 &
    ```

  * node03

    ```
    nohup /export/servers/kafka/bin/kafka-server-start.sh /export/servers/kafka/config/server.properties >/dev/null 2>&1 &
    ```

## 2、集群操作

### 2.1、需求

​	1）电商网站中，有个电商订单服务模块。当用户下单之后，要发送一个消息出来。

​	  生产者： 订单信息

​	2）使用kafka集群来接受消息。

​	topic：order

​	3） 消费订单信息。

​	消费者。

### 2.2、使用控制台的方式

* 创建topic
  * bin/kafka-topics.sh --create --zookeeper node01:2181 --replication-factor 1 --partitions 1 --topic order
* 查看集群所有的topic
  * bin/kafka-topics.sh --list --zookeeper node01:2181 
* 生产者
  * bin/kafka-console-producer.sh  --broker-list node01:9092 --topic order 
* 消费者
  * bin/kafka-console-consumer.sh  --bootstrap-server node01:9092 --from-beginning --topic order



### 2.3、Java Api的方式

* 创建一个项目（maven）

  * 导入依赖

    ```xml
    		 <dependency>
                <groupId>org.apache.kafka</groupId>
                <artifactId>kafka-clients</artifactId>
                <version>0.11.0.1</version>
            </dependency>
    ```

    ​

* 编写代码

  * 生产者   KafkaProducer

    ```java
    public static void main(String[] args) throws InterruptedException {
            /**
             *  连接kafka集群
             *      broker1，broker2，broker3
             *      node01,node02,node03,-------------------linux
             *      ------如果代码中制定了域名的方式访问，需要在开发环境中设置hosts
             *  生产数据
             *      topic：order
             *      数据：订单1，订单2，订单3
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

            // 1.创建KafkaProducer 也就kafka的生产者
            // 1.1 需要一个Properties对象--怎么连接kafka集群
            KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(props);
            // 2.发送数据----ProducerRecord 封装数据
            for(int i=0;i<=2000;i++){
                // 2.2 在构造ProducerRecord发现有很多构造参数
                // 选择参数最少的一个，有两个参数，第一个topic，第二个是内容
                ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("order","订单"+i);
                // 2.1 发送ProducerRecord对象
                kafkaProducer.send(producerRecord);
            }
            // 发现一个问题，程序运行完，但是consumer并没有消费到数据。必须让程序休眠一段时间，后才有消费。
            // kafka 消息不丢失机制。
            Thread.sleep(3000);
        }
    ```

    ​

  * 消费者 KafkaConsumer

    ```java
     public static void main(String[] args) {
            // 1.连接集群，订阅topic，准备消费数据
            // 2.消费数据
            Properties props = new Properties();
            props.put("bootstrap.servers", "node01:9092");
            props.put("group.id", "test");
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", "1000");
            props.put("key.deserializer",
                    "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer",
                    "org.apache.kafka.common.serialization.StringDeserializer");

            // 1.创建KafkaConsumer
            // 1.1 发现KafkaConsumer 也需要一个 Properties
            KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(props);
            // 1.2 订阅数据
            kafkaConsumer.subscribe(Arrays.asList("order"));
            while (true) {
                // 2. 消费数据
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(100);
                // 2.1 迭代ConsumerRecords 获取数据
                // 有topic的名字，key，value，partition的内容
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    System.out.println("topic: " + consumerRecord.topic());
                    System.out.println("value: " + consumerRecord.value());
                    System.out.println("----------------------");
                }
            }
        }
    ```

