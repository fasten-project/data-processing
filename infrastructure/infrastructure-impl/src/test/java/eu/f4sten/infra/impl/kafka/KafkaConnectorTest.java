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

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static eu.f4sten.infra.kafka.Lane.PRIORITY;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_INSTANCE_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
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

import eu.f4sten.infra.AssertArgsError;
import eu.f4sten.infra.LoaderArgs;
import eu.f4sten.infra.impl.InfraArgs;
import eu.f4sten.infra.kafka.Lane;

public class KafkaConnectorTest {

    private static final String KAFKA_URL = "1.2.3.4:1234";
    private static final String SOME_PLUGIN = "p";

    private LoaderArgs loaderArgs;
    private InfraArgs infraArgs;
    private KafkaConnector sut;

    @BeforeEach
    public void setup() {
        loaderArgs = new LoaderArgs();
        loaderArgs.plugin = SOME_PLUGIN;
        infraArgs = new InfraArgs();
        infraArgs.kafkaUrl = KAFKA_URL;
        sut = new KafkaConnector(loaderArgs, infraArgs);
    }

    @Test
    public void inputValidationKafkaUrlMustNotBeNull() throws Exception {
        var infraArgs = new InfraArgs();
        var out = tapSystemOut(() -> {
            assertThrows(AssertArgsError.class, () -> {
                new KafkaConnector(loaderArgs, infraArgs);
            });
        });
        assertTrue(out.contains("(kafka url)"));
    }

    @Test
    public void inputValidationInstanceIdMustNotBeEmpty() throws Exception {
        infraArgs.instanceId = null; // default!
        new KafkaConnector(loaderArgs, infraArgs);

        infraArgs.instanceId = "";
        var out = tapSystemOut(() -> {
            assertThrows(AssertArgsError.class, () -> {
                new KafkaConnector(loaderArgs, infraArgs);
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
            expected.setProperty(BOOTSTRAP_SERVERS_CONFIG, infraArgs.kafkaUrl);
            expected.setProperty(GROUP_ID_CONFIG, l == Lane.PRIORITY //
                    ? SOME_PLUGIN + "-prio" //
                    : SOME_PLUGIN);
            expected.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
            expected.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            expected.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            expected.setProperty(FETCH_MAX_BYTES_CONFIG, Integer.toString(50 * 1024 * 1024));
            expected.setProperty(MAX_POLL_RECORDS_CONFIG, "1");
            expected.setProperty(MAX_POLL_INTERVAL_MS_CONFIG, Integer.toString(1000 * 60 * 30));
            expected.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "false");
            expected.setProperty(AUTO_COMMIT_INTERVAL_MS_CONFIG, "0");

            assertEquals(expected, actual);
        }
    }

    @Test
    public void groupIdRemovesPartialPackage() {

        loaderArgs.plugin = "eu.f4sten.p";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        var actual = sut.getConsumerProperties(PRIORITY).getProperty(GROUP_ID_CONFIG);
        var expected = "p-prio";
        assertEquals(expected, actual);
    }

    @Test
    public void groupIdRemovesMainClass() {

        loaderArgs.plugin = "q.Main";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        var actual = sut.getConsumerProperties(PRIORITY).getProperty(GROUP_ID_CONFIG);
        var expected = "q-prio";
        assertEquals(expected, actual);
    }

    @Test
    public void groupIdDoesNotRemoveMainInTheMiddle() {

        loaderArgs.plugin = "q.Main.foo";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        var actual = sut.getConsumerProperties(PRIORITY).getProperty(GROUP_ID_CONFIG);
        var expected = "q.Main.foo-prio";
        assertEquals(expected, actual);
    }

    @Test
    public void instanceIdIsSetNormal() {
        infraArgs.instanceId = "X";
        var expected = "p-X";
        var actual = sut.getConsumerProperties(Lane.NORMAL);
        assertEquals(expected, actual.get(CLIENT_ID_CONFIG));
        assertEquals(expected, actual.get(GROUP_INSTANCE_ID_CONFIG));
    }

    @Test
    public void instanceIdIsSetPriority() {
        infraArgs.instanceId = "X";
        var expected = "p-X-prio";
        var actual = sut.getConsumerProperties(Lane.PRIORITY);
        assertEquals(expected, actual.get(CLIENT_ID_CONFIG));
        assertEquals(expected, actual.get(GROUP_INSTANCE_ID_CONFIG));
    }

    @Test
    public void instanceIdRemovesPartialPackage() {
        loaderArgs.plugin = "eu.f4sten.p";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        infraArgs.instanceId = "X";
        var expected = "p-X-prio";
        var actual = sut.getConsumerProperties(Lane.PRIORITY);
        assertEquals(expected, actual.get(CLIENT_ID_CONFIG));
        assertEquals(expected, actual.get(GROUP_INSTANCE_ID_CONFIG));
    }

    @Test
    public void instanceIdRemovesMainClass() {
        loaderArgs.plugin = "q.Main";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        infraArgs.instanceId = "X";
        var expected = "q-X-prio";
        var actual = sut.getConsumerProperties(Lane.PRIORITY);
        assertEquals(expected, actual.get(CLIENT_ID_CONFIG));
        assertEquals(expected, actual.get(GROUP_INSTANCE_ID_CONFIG));
    }

    @Test
    public void instanceIdDoesNotRemoveMainInTheMiddle() {
        loaderArgs.plugin = "q.Main.foo";
        sut = new KafkaConnector(loaderArgs, infraArgs);

        infraArgs.instanceId = "X";
        var expected = "q.Main.foo-X-prio";
        var actual = sut.getConsumerProperties(Lane.PRIORITY);
        assertEquals(expected, actual.get(CLIENT_ID_CONFIG));
        assertEquals(expected, actual.get(GROUP_INSTANCE_ID_CONFIG));
    }

    @Test
    public void instanceCanOnlyBeUsedOnce() {
        infraArgs.instanceId = "X";
        sut.getConsumerProperties(NORMAL);
        assertThrows(InvalidParameterException.class, () -> {
            sut.getConsumerProperties(NORMAL);
        });
    }

    @Test
    public void smokeTestCanConstructProducerAndConsumer() {
        assertNotNull(sut.getConsumerConnection(NORMAL));
        assertNotNull(sut.getConsumerConnection(PRIORITY));
        assertNotNull(sut.getProducerConnection());
    }

    @Test
    public void smokeTestCanConstructProducerAndConsumerWithInstanceId() {
        infraArgs.instanceId = "X";
        assertNotNull(sut.getConsumerConnection(NORMAL));
        assertNotNull(sut.getConsumerConnection(PRIORITY));
        assertNotNull(sut.getProducerConnection());
    }
}