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
package eu.f4sten.server.kafka;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static eu.f4sten.server.core.kafka.Lane.NORMAL;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_INSTANCE_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidParameterException;
import java.util.Properties;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.stefanbirkner.systemlambda.SystemLambda;

import eu.f4sten.server.ServerArgs;
import eu.f4sten.server.core.kafka.Lane;

public class KafkaConnectorTest {

    private static final String KAFKA_URL = "1.2.3.4:1234";

    private ServerArgs args;
    private KafkaConnector sut;

    @BeforeEach
    public void setup() {
        args = new ServerArgs();
        args.plugin = "p";
        args.kafkaUrl = KAFKA_URL;
        sut = new KafkaConnector(args);
    }

    @Test
    public void inputValidationKafkaUrlMustNotBeNull() throws Exception {
        var args = new ServerArgs();
        var out = tapSystemOut(() -> {
            SystemLambda.catchSystemExit(() -> {
                new KafkaConnector(args);
            });
        });
        assertTrue(out.contains("(kafka url)"));
    }

    @Test
    public void inputValidationInstanceIdMustNotBeEmpty() throws Exception {
        ServerArgs args = new ServerArgs();
        args.kafkaUrl = KAFKA_URL;
        args.instanceId = null;
        new KafkaConnector(args);

        args.instanceId = "";
        var out = tapSystemOut(() -> {
            catchSystemExit(() -> {
                new KafkaConnector(args);
            });
        });
        assertTrue(out.contains("(instance id must be null or non-empty)"));
    }

    @Test
    public void checkDefaultProducerProperties() {
        var actual = sut.getProducerProperties();

        Properties expected = new Properties();
        expected.setProperty(BOOTSTRAP_SERVERS_CONFIG, KAFKA_URL);
        expected.setProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        expected.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        expected.setProperty(MAX_REQUEST_SIZE_CONFIG, Integer.toString(50 * 1024 * 1024));

        assertEquals(expected, actual);
    }

    @Test
    public void checkDefaultConsumerProperties() {
        for (var l : Lane.values()) {
            var actual = sut.getConsumerProperties(l);

            Properties expected = new Properties();
            expected.setProperty(BOOTSTRAP_SERVERS_CONFIG, args.kafkaUrl);
            expected.setProperty(GROUP_ID_CONFIG, args.plugin);
            expected.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
            expected.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            expected.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            expected.setProperty(FETCH_MAX_BYTES_CONFIG, Integer.toString(50 * 1024 * 1024));

            assertEquals(expected, actual);
        }
    }

    @Test
    public void instanceIdIsSet() {
        args.instanceId = "X";
        for (var l : Lane.values()) {
            var actual = sut.getConsumerProperties(l).get(GROUP_INSTANCE_ID_CONFIG);
            var expected = "p-X-" + l;
            assertEquals(expected, actual);
        }
    }

    @Test
    public void instanceCanOnlyBeUsedOnce() {
        args.instanceId = "X";
        sut.getConsumerProperties(NORMAL);
        assertThrows(InvalidParameterException.class, () -> {
            sut.getConsumerProperties(NORMAL);
        });
    }

    @Test
    public void smokeTestCanConstructProducerAndConsumer() {
        assertNotNull(sut.getConsumerConnection(NORMAL));
        assertNotNull(sut.getProducerConnection());
    }
}