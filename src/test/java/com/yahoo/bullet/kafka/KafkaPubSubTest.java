/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.kafka;

import com.yahoo.bullet.common.BulletConfig;
import com.yahoo.bullet.pubsub.PubSub;
import com.yahoo.bullet.pubsub.PubSubException;
import com.yahoo.bullet.pubsub.PubSubMessage;
import com.yahoo.bullet.pubsub.Publisher;
import com.yahoo.bullet.pubsub.Subscriber;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.yahoo.bullet.kafka.TestUtils.makeConsumerRecords;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class KafkaPubSubTest {
    private static final String MAX_BLOCK_MS = KafkaConfig.PRODUCER_NAMESPACE + "max.block.ms";

    List<TopicPartition> requestPartitions = Arrays.asList(new TopicPartition("bullet.queries", 0),
                                                           new TopicPartition("bullet.queries", 1),
                                                           new TopicPartition("bullet.queries", 2),
                                                           new TopicPartition("bullet.queries", 3));

    List<TopicPartition> responsePartitions = Arrays.asList(new TopicPartition("bullet.responses", 4),
                                                            new TopicPartition("bullet.responses", 5),
                                                            new TopicPartition("bullet.responses", 6),
                                                            new TopicPartition("bullet.responses", 7));

    @Test
    public void testSwitchContext() throws PubSubException {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));

        Assert.assertEquals(kafkaPubSub.getContext(), PubSub.Context.QUERY_SUBMISSION);

        kafkaPubSub.switchContext(PubSub.Context.QUERY_PROCESSING, config);
        Assert.assertEquals(kafkaPubSub.getContext(), PubSub.Context.QUERY_PROCESSING);
    }

    @Test
    public void testQuerySubmissionPartitions() throws PubSubException {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));

        KafkaQueryPublisher publisher = (KafkaQueryPublisher) kafkaPubSub.getPublisher();
        Assert.assertEquals(requestPartitions, publisher.getWritePartitions());
        Assert.assertEquals(responsePartitions, publisher.getReceivePartitions());
        publisher.close();

        KafkaSubscriber subscriber = (KafkaSubscriber) kafkaPubSub.getSubscriber();
        KafkaConsumer<String, byte[]> consumer = subscriber.getConsumer();
        Assert.assertEquals(consumer.assignment(), new HashSet<>(responsePartitions));
        consumer.close();
    }

    @Test
    public void testQueryProcessingPartitions() throws PubSubException {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));

        KafkaSubscriber subscriber = (KafkaSubscriber) kafkaPubSub.getSubscriber();
        KafkaConsumer<String, byte[]> consumer = subscriber.getConsumer();
        Assert.assertEquals(consumer.assignment(), new HashSet<>(requestPartitions));
        consumer.close();
    }

    @Test(expectedExceptions = PubSubException.class)
    public void testIllegalRequestPartitions() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        config.set(KafkaConfig.REQUEST_PARTITIONS, "");
        config.set(BulletConfig.PUBSUB_CLASS_NAME, "com.yahoo.bullet.kafka.KafkaPubSub");
        PubSub.from(config);
    }

    @Test(expectedExceptions = PubSubException.class)
    public void testIllegalResponsePartitions() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        config.set(KafkaConfig.RESPONSE_PARTITIONS, "");
        config.set(BulletConfig.PUBSUB_CLASS_NAME, "com.yahoo.bullet.kafka.KafkaPubSub");
        PubSub.from(config);
    }

    @Test
    public void testGetSubscribers() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        config.set(KafkaConfig.RATE_LIMIT_ENABLE, false);
        config.set(KafkaConfig.RATE_LIMIT_MAX_MESSAGES, 5);
        config.set(KafkaConfig.RATE_LIMIT_INTERVAL_MS, 1000);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Subscriber> subscribers = kafkaPubSub.getSubscribers(10);
        Assert.assertEquals(subscribers.size(), 4);

        // Confirming that these subscribers are not rate limited.
        KafkaConsumer<String, byte[]> mockConsumer = (KafkaConsumer<String, byte[]>) Mockito.mock(KafkaConsumer.class);
        ConsumerRecords<String, byte[]> records = makeConsumerRecords("id", new PubSubMessage("id", "message", null));
        when(mockConsumer.poll(any())).thenReturn(records);
        for (Subscriber subscriber : subscribers) {
            KafkaSubscriber kafkaSubscriber = (KafkaSubscriber) subscriber;
            kafkaSubscriber.setConsumer(mockConsumer);
            for (int i = 0; i < 10; i++) {
                Assert.assertNotNull(kafkaSubscriber.receive());
            }
        }
    }

    @Test
    public void testGetSubscribersWithRateLimit() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        config.set(KafkaConfig.RATE_LIMIT_ENABLE, true);
        config.set(KafkaConfig.RATE_LIMIT_MAX_MESSAGES, 5);
        config.set(KafkaConfig.RATE_LIMIT_INTERVAL_MS, 1000);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Subscriber> subscribers = kafkaPubSub.getSubscribers(10);
        Assert.assertEquals(subscribers.size(), 4);
        // Confirming that these subscribers are rate limited.
        KafkaConsumer<String, byte[]> mockConsumer = (KafkaConsumer<String, byte[]>) Mockito.mock(KafkaConsumer.class);
        ConsumerRecords<String, byte[]> records = makeConsumerRecords("id", new PubSubMessage("id", "message", null));
        when(mockConsumer.poll(any())).thenReturn(records);
        for (Subscriber subscriber : subscribers) {
            KafkaSubscriber kafkaSubscriber = (KafkaSubscriber) subscriber;
            kafkaSubscriber.setConsumer(mockConsumer);
            for (int i = 0; i < 5; i++) {
                Assert.assertNotNull(kafkaSubscriber.receive());
            }
            Assert.assertNull(kafkaSubscriber.receive());
        }
    }

    @Test
    public void testGetPublishers() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Publisher> publishers = kafkaPubSub.getPublishers(10);
        Assert.assertEquals(publishers.size(), 10);
        for (Publisher publisher : publishers) {
            Assert.assertTrue(publisher instanceof KafkaResponsePublisher);
        }
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*" + KafkaConfig.BOOTSTRAP_SERVERS + ".*")
    public void testMissingRequiredProperties() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaConfig kafkaConfig = new KafkaConfig(config);
        kafkaConfig.set(KafkaConfig.BOOTSTRAP_SERVERS, null);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(kafkaConfig);
        kafkaPubSub.getPublisher();
    }

    @Test(expectedExceptions = PubSubException.class)
    public void testMalformedPartitionList() throws Exception {
        BulletConfig config = new BulletConfig("test_malformed_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        config.set(BulletConfig.PUBSUB_CLASS_NAME, "com.yahoo.bullet.kafka.KafkaPubSub");
        PubSub.from(config);
    }

    @Test
    public void testNoResponsePartitions() throws Exception {
        BulletConfig config = new BulletConfig("test_config_no_partitions.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        KafkaSubscriber subscriber = (KafkaSubscriber) kafkaPubSub.getSubscriber();
        Assert.assertEquals(subscriber.getConsumer().subscription(), singletonList("bullet.responses"));
    }

    @Test
    public void testNoRequestPartitions() throws Exception {
        BulletConfig config = new BulletConfig("test_config_no_partitions.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        KafkaSubscriber subscriber = (KafkaSubscriber) kafkaPubSub.getSubscriber();
        Assert.assertEquals(subscriber.getConsumer().subscription(), singletonList("bullet.queries"));
    }

    @Test
    public void testSubscriberPartitionAllocationWhenExact() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Subscriber> subscriber = kafkaPubSub.getSubscribers(4);
        Assert.assertEquals(subscriber.size(), 4);
        Assert.assertTrue(subscriber.stream().mapToInt(x -> ((KafkaSubscriber) x).getConsumer().assignment().size()).allMatch(x -> x == 1));
    }

    @Test
    public void testSubscriberPartitionsAllocationWhenInsufficient() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Subscriber> subscriber = kafkaPubSub.getSubscribers(5);
        // Test that every subscriber is allocated one partition and the size of the list is the allocated number of partitions.
        Assert.assertEquals(subscriber.size(), 4);
        Assert.assertTrue(subscriber.stream()
                                    .mapToInt(x -> ((KafkaSubscriber) x).getConsumer().assignment().size())
                                    .allMatch(x -> x == 1));
    }

    @Test
    public void testSubscriberPartitionsAllocationWhenInExcess() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_PROCESSING");
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        List<Subscriber> subscriber = kafkaPubSub.getSubscribers(3);
        Assert.assertEquals(subscriber.size(), 2);
        Assert.assertTrue(subscriber.stream()
                                    .mapToInt(x -> ((KafkaSubscriber) x).getConsumer().assignment().size())
                                    .allMatch(x -> x == 2));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNoQueryPartitionsWhenUnableToReachKafka() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        config.set(KafkaConfig.REQUEST_PARTITIONS, null);
        config.set(MAX_BLOCK_MS, 50);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        kafkaPubSub.getPublisher();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNoResponsePartitionsWhenUnableToReachKafka() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        config.set(KafkaConfig.RESPONSE_PARTITIONS, null);
        config.set(MAX_BLOCK_MS, 50);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        kafkaPubSub.getPublisher();
    }

    @Test
    public void testGetSubscribersWhenNoPartitions() throws Exception {
        BulletConfig config = new BulletConfig("test_config.yaml");
        config.set(BulletConfig.PUBSUB_CONTEXT_NAME, "QUERY_SUBMISSION");
        config.set(KafkaConfig.RESPONSE_PARTITIONS, null);
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new KafkaConfig(config));
        kafkaPubSub.getSubscribers(5).forEach(x -> Assert.assertEquals(((KafkaSubscriber) x).getConsumer().subscription(), Collections.singleton("bullet.responses")));
    }

    @Test
    public void testGetAllPartitions() throws Exception {
        KafkaProducer<String, byte[]> producer = Mockito.mock(KafkaProducer.class);
        PartitionInfo dummy = new PartitionInfo("bullet", 0, null, null, null);
        Mockito.when(producer.partitionsFor(anyString())).thenReturn(Arrays.asList(dummy));
        KafkaPubSub kafkaPubSub = new KafkaPubSub(new BulletConfig("test_config.yaml"));
        List<TopicPartition> partitions = kafkaPubSub.getAllPartitions(producer, "");
        Assert.assertEquals(partitions.size(), 1);
        Assert.assertEquals(partitions.get(0), new TopicPartition("bullet", 0));
    }
}
