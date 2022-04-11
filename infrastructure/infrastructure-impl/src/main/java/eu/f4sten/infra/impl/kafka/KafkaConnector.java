/*
 * Copyright 2021 Delft University of Technology
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.infra.impl.kafka;

import static eu.f4sten.infra.AssertArgs.assertFor;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_INSTANCE_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.LoaderArgs;
import eu.f4sten.infra.impl.InfraArgs;
import eu.f4sten.infra.kafka.Lane;

public class KafkaConnector {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnector.class);

    private static final String MAX_REQUEST_SIZE = valueOf(50 * 1024 * 1024); // 50MB
    private static final String MAX_POLL_INTERVAL_MS = valueOf(1000 * 60 * 30); // 30min

    private final String activePlugin;
    private final InfraArgs args;
    private final Set<String> instanceIds = new HashSet<>();

    @Inject
    public KafkaConnector(LoaderArgs loaderArgs, InfraArgs infraArgs) {
        var activePlugin = loaderArgs.plugin.replace("eu.f4sten.", "");
        this.activePlugin = activePlugin.endsWith(".Main") //
                ? activePlugin.replace(".Main", "") //
                : activePlugin;
        this.args = infraArgs;
        assertFor(infraArgs) //
                .notNull(a -> a.kafkaUrl, "kafka url") //
                .that(a -> a.instanceId == null || !a.instanceId.isEmpty(), "instance id must be null or non-empty");
    }

    public KafkaConsumer<String, String> getConsumerConnection(Lane l) {
        return new KafkaConsumer<>(getConsumerProperties(l));
    }

    public Properties getConsumerProperties(Lane l) {
        Properties p = new Properties();
        p.setProperty(BOOTSTRAP_SERVERS_CONFIG, args.kafkaUrl);
        p.setProperty(GROUP_ID_CONFIG, getGroupId(l));
        p.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(FETCH_MAX_BYTES_CONFIG, MAX_REQUEST_SIZE);
        p.setProperty(MAX_POLL_RECORDS_CONFIG, "1");
        p.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.setProperty(AUTO_COMMIT_INTERVAL_MS_CONFIG, "0");

        p.setProperty(MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS);

        var instanceId = getFullInstanceId(l);
        if (instanceId != null) {
            if (instanceIds.contains(instanceId)) {
                throw new InvalidParameterException("instance id already exists " + instanceId);
            }
            instanceIds.add(instanceId);
            p.setProperty(CLIENT_ID_CONFIG, instanceId);
            p.setProperty(GROUP_INSTANCE_ID_CONFIG, instanceId);
            LOG.info("Enabling static membership (instance id: {})", instanceId);
        }
        return p;
    }

    private String getGroupId(Lane lane) {
        var suffix = lane == Lane.PRIORITY ? "-prio" : "";
        return format("%s%s", activePlugin, suffix);
    }

    private String getFullInstanceId(Lane lane) {
        if (args.instanceId == null) {
            return null;
        }
        var suffix = lane == Lane.PRIORITY ? "-prio" : "";
        return format("%s-%s%s", activePlugin, args.instanceId, suffix);
    }

    public KafkaProducer<String, String> getProducerConnection() {
        return new KafkaProducer<>(getProducerProperties());
    }

    public Properties getProducerProperties() {
        Properties p = new Properties();
        p.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, args.kafkaUrl);
        p.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, MAX_REQUEST_SIZE);
        return p;
    }
}