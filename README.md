# Digest

This repo contains a couple of basic applications using Spark Structured Streaming:

- DigestKafka: Reads from a Kafka topic and writes it to a different Kafka topic
- DigestHDFS: Reads from a Kafka topic and writes it to a specific path in HDFS as JSON.

## Configuration

All the paramaters are mandatory. Defaults are meant to be used only for local testing.

### Digest Kafka

All the parameter names must be prepended by `digestkafka.`

**Spark configuration**

| Param               | Description                          | Default      |
|:--------------------|:-------------------------------------|:-------------|
| app.name            | Spark application name               | digest-kafka |
| master              | Spark deployment type                | local\[*\]   |
| checkpoint.location | Directory to store spark checkpoints | /tmp/kafka   |

**Kafka configuration**

| Param               | Description                                     | Default        |
|:--------------------|:------------------------------------------------|:---------------|
| bootstrap.servers   | Comma separated list of kafka bootstrap servers | localhost:9092 |
| input.topic         | Kafka topic used to read data from              | input-topic    |
| output.topic        | Kafka topic used to write data to               | output-topic   |

### Digest HDFS

All the parameter names must be prepended by `digesthdfs.`

**Spark configuration**

| Param               | Description                          | Default      |
|:--------------------|:-------------------------------------|:-------------|
| app.name            | Spark application name               | digest-hdfs  |
| master              | Spark deployment type                | local\[*\]   |
| checkpoint.location | Directory to store spark checkpoints | /tmp/hdfs    |

**Kafka configuration**

| Param               | Description                                     | Default        |
|:--------------------|:------------------------------------------------|:---------------|
| bootstrap.servers   | Comma separated list of kafka bootstrap servers | localhost:9092 |
| input.topic         | Kafka topic used to read data from              | input-topic    |

**HDFS configuration**

| Param      | Description                                         | Default          |
|:-----------|:----------------------------------------------------|:-----------------|
| output.dir | Location in HDFS where the output should be stored  | /tmp/output/hdfs |

## Run locally

To run the applications locally you must have [Kafka](https://kafka.apache.org/quickstart) running in your environment.

```
cd kafka_2.11-1.0.0
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties

```

Create input and output topics:

```
kafka_2.11-1.0.0
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic input-topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic output-topic
```

You can use `kafka-console-consumer.sh` to read data from a Kafka topic:

```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic input-topic
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic output-topic
```

and `kafka-console-producer.sh` to send data to a Kafka topic:

```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic input-topic
```

### Run DigestKafka

```
sbt run
```

Once the application has started, use the `kafka-console-consumer.sh` to read data from `output-topic` to observe the output of the application. Use `kafka-console-producer.sh` to send data to the application.

As soon as you send data to Kafka, you should be able to read the same as output on the `kafka-console-consumer.sh` console.

Abort the Spark streaming application with `Ctrl-C`.

### Run DigestHDFS

Note that in the absence of HDFS, the application will write the output to your local system.

```
sbt run
```

Once the application has started, use the `kafka-console-producer.sh` to send data to the application.

As soon as you send data to Kafka, data will be stored on the given path. Check the JSON files created under the directory.

Abort the Spark streaming application with `Ctrl-C`.

## Run in a cluster

To submit a spark application you must first build an assembly jar:

```
sbt publish-local
```

It will be published to: `~/.ivy2/local/default/digest_2.11/1.0.0/jars/digest_2.11-assembly.jar`.

You need to transfer the assembly jar to the cluster:

```
scp ~/.ivy2/local/default/digest_2.11/1.0.0/jars/digest_2.11-assembly.jar your_cluster:
```

Then you can run a `spark-submit` command in the cluster:

### Run DigestKafka in a (tiny) YARN cluster

```
spark-submit \
--class com.playground.streaming.DigestKafka \
--master yarn \
--deploy-mode cluster \
--driver-memory 512m \
--executor-memory 512m \
--executor-cores 1 \
digest_2.11-assembly.jar \
bootstrap.servers=my.kafka:9092 input.topic=data-in output.topic=data-out
```

### Run DigestHDFS in a (tiny) YARN cluster

```
spark-submit \
--class com.playground.streaming.DigestHDFS \
--master yarn \
--deploy-mode cluster \
--driver-memory 512m \
--executor-memory 512m \
--executor-cores 1 \
digest_2.11-assembly.jar \
bootstrap.servers=my.kafka:9092 input.topic=data-in output.dir=/tmp/data-out
```

## TODO

- [ ] Add tests.
