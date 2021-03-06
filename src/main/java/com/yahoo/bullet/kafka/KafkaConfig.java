/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.kafka;

import com.yahoo.bullet.common.BulletConfig;
import com.yahoo.bullet.common.Config;
import com.yahoo.bullet.common.Validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KafkaConfig extends BulletConfig {
    public static final String KAFKA_NAMESPACE = "bullet.pubsub.kafka" + DELIMITER;

    // Common managed Kafka properties
    public static final String BOOTSTRAP_SERVERS = KAFKA_NAMESPACE + "bootstrap.servers";
    public static final String CONNECTIONS_MAX_IDLE_MS = KAFKA_NAMESPACE + "connections.max.idle.ms";

    public static final Set<String> COMMON_PROPERTIES =
        new HashSet<>(Arrays.asList(BOOTSTRAP_SERVERS, CONNECTIONS_MAX_IDLE_MS));

    // Producer and consumer properties
    public static final String SSL_CERT_LOCATION = "ssl.cert.refreshing.cert.location";
    public static final String SSL_KEY_LOCATION = "ssl.cert.refreshing.key.location";
    public static final String SSL_KEY_REFRESH_INTERVAL = "ssl.cert.refreshing.refresh.interval.ms";

    public static final String PRODUCER_NAMESPACE = KAFKA_NAMESPACE + "producer" + DELIMITER;
    // Producer specific properties
    public static final String KEY_SERIALIZER = PRODUCER_NAMESPACE + "key.serializer";
    public static final String VALUE_SERIALIZER = PRODUCER_NAMESPACE + "value.serializer";
    public static final String PRODUCER_SSL_CERT_LOCATION = PRODUCER_NAMESPACE + SSL_CERT_LOCATION;
    public static final String PRODUCER_SSL_KEY_LOCATION = PRODUCER_NAMESPACE + SSL_KEY_LOCATION;
    public static final String PRODUCER_SSL_KEY_REFRESH_INTERVAL = PRODUCER_NAMESPACE + SSL_KEY_REFRESH_INTERVAL;

    public static final String CONSUMER_NAMESPACE = KAFKA_NAMESPACE + "consumer" + DELIMITER;
    // Consumer specific properties
    public static final String GROUP_ID = CONSUMER_NAMESPACE + "group.id";
    public static final String ENABLE_AUTO_COMMIT = CONSUMER_NAMESPACE + "enable.auto.commit";
    public static final String KEY_DESERIALIZER = CONSUMER_NAMESPACE + "key.deserializer";
    public static final String VALUE_DESERIALIZER = CONSUMER_NAMESPACE + "value.deserializer";
    public static final String CONSUMER_SSL_CERT_LOCATION = CONSUMER_NAMESPACE + SSL_CERT_LOCATION;
    public static final String CONSUMER_SSL_KEY_LOCATION = CONSUMER_NAMESPACE + SSL_KEY_LOCATION;
    public static final String CONSUMER_SSL_KEY_REFRESH_INTERVAL = CONSUMER_NAMESPACE + SSL_KEY_REFRESH_INTERVAL;

    // Kafka PubSub properties
    public static final String REQUEST_PARTITIONS = KAFKA_NAMESPACE + "request.partitions";
    public static final String RESPONSE_PARTITIONS = KAFKA_NAMESPACE + "response.partitions";
    public static final String REQUEST_TOPIC_NAME = KAFKA_NAMESPACE + "request.topic.name";
    public static final String RESPONSE_TOPIC_NAME = KAFKA_NAMESPACE + "response.topic.name";
    public static final String PARTITION_ROUTING_ENABLE = KAFKA_NAMESPACE + "partition.routing.enable";

    // Kafka PubSub Subscriber properties
    public static final String MAX_UNCOMMITTED_MESSAGES = KAFKA_NAMESPACE + "subscriber.max.uncommitted.messages";
    public static final String RATE_LIMIT_ENABLE = KAFKA_NAMESPACE + "subscriber.rate.limit.enable";
    public static final String RATE_LIMIT_MAX_MESSAGES = KAFKA_NAMESPACE + "subscriber.rate.limit.max.messages";
    public static final String RATE_LIMIT_INTERVAL_MS = KAFKA_NAMESPACE + "subscriber.rate.limit.interval.ms";

    // Defaults
    private static String TRUE = "true";
    private static String FALSE = "false";
    public static final String DEFAULT_KAFKA_CONFIGURATION = "bullet_kafka_defaults.yaml";
    public static final String DEFAULT_ENABLE_AUTO_COMMIT = TRUE;
    public static final boolean DEFAULT_PARTITION_ROUTING_ENABLE = true;
    public static final boolean DEFAULT_RATE_LIMIT_ENABLE = false;

    private static final long serialVersionUID = 7613682421100044732L;
    private static final Validator VALIDATOR = BulletConfig.getValidator();

    static {
        VALIDATOR.define(BOOTSTRAP_SERVERS)
                 .checkIf(Validator::isString)
                 .orFail();
        VALIDATOR.define(REQUEST_TOPIC_NAME)
                 .checkIf(Validator::isString)
                 .orFail();
        VALIDATOR.define(RESPONSE_TOPIC_NAME)
                 .checkIf(Validator::isString)
                 .orFail();
        VALIDATOR.define(REQUEST_PARTITIONS)
                 .checkIf(Validator.isListOfType(Integer.class))
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(RESPONSE_PARTITIONS)
                 .checkIf(Validator.isListOfType(Integer.class))
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(KEY_SERIALIZER)
                 .checkIf(Validator::isClassName)
                 .orFail();
        VALIDATOR.define(VALUE_SERIALIZER)
                 .checkIf(Validator::isClassName)
                 .orFail();
        VALIDATOR.define(KEY_DESERIALIZER)
                 .checkIf(Validator::isClassName)
                 .orFail();
        VALIDATOR.define(VALUE_DESERIALIZER)
                 .checkIf(Validator::isClassName)
                 .orFail();
        VALIDATOR.define(GROUP_ID)
                 .checkIf(Validator::isString)
                 .orFail();
        VALIDATOR.define(ENABLE_AUTO_COMMIT)
                 .checkIf(Validator::isString)
                 .checkIf(Validator.isIn(TRUE, FALSE))
                 .defaultTo(DEFAULT_ENABLE_AUTO_COMMIT);
        VALIDATOR.define(PRODUCER_SSL_CERT_LOCATION)
                 .checkIf(Validator::isString)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(PRODUCER_SSL_KEY_LOCATION)
                 .checkIf(Validator::isString)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(PRODUCER_SSL_KEY_REFRESH_INTERVAL)
                 .checkIf(Validator::isPositiveInt)
                 .castTo(Validator::asInt)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(CONSUMER_SSL_CERT_LOCATION)
                 .checkIf(Validator::isString)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(CONSUMER_SSL_KEY_LOCATION)
                 .checkIf(Validator::isString)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(CONSUMER_SSL_KEY_REFRESH_INTERVAL)
                 .checkIf(Validator::isPositiveInt)
                 .castTo(Validator::asInt)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(PARTITION_ROUTING_ENABLE)
                 .checkIf(Validator::isBoolean)
                 .defaultTo(DEFAULT_PARTITION_ROUTING_ENABLE);
        VALIDATOR.define(RATE_LIMIT_ENABLE)
                 .checkIf(Validator::isBoolean)
                 .defaultTo(DEFAULT_RATE_LIMIT_ENABLE);
        VALIDATOR.define(RATE_LIMIT_MAX_MESSAGES)
                 .checkIf(Validator::isPositiveInt)
                 .unless(Validator::isNull)
                 .orFail();
        VALIDATOR.define(RATE_LIMIT_INTERVAL_MS)
                 .checkIf(Validator::isPositiveInt)
                 .unless(Validator::isNull)
                 .orFail();
    }

    /**
     * Creates a KafkaConfig by reading in a file.
     *
     * @param file The file to read in to create the KafkaConfig.
     */
    public KafkaConfig(String file) {
        this(new Config(file));
    }

    /**
     * Creates a KafkaConfig from a Config.
     *
     * @param config The {@link Config} to copy settings from.
     */
    public KafkaConfig(Config config) {
        // Load default Kafka settings. Merge additional settings in Config
        super(DEFAULT_KAFKA_CONFIGURATION);
        merge(config);
    }

    @Override
    public BulletConfig validate() {
        VALIDATOR.validate(this);
        return this;
    }
}
