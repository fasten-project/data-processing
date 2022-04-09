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

import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static eu.f4sten.infra.kafka.Lane.PRIORITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Lane;

public class KafkaImplTest {

    private static final TRef<String> T_STRING = new TRef<String>() {};
    private static final BiConsumer<String, Lane> SOME_CB = (s, l) -> {};
    private static final BiFunction<String, Throwable, ?> SOME_ERR_CB = (s, t) -> null;

    private JsonUtils jsonUtils;
    private KafkaConnector connector;
    private KafkaConsumer<String, String> consumerNorm;
    private KafkaConsumer<String, String> consumerPrio;
    private KafkaProducer<String, String> producer;

    private Map<Object, String> jsons = new HashMap<>();

    private KafkaImpl sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        jsonUtils = mock(JsonUtils.class);

        connector = mock(KafkaConnector.class);
        consumerNorm = mock(KafkaConsumer.class);
        consumerPrio = mock(KafkaConsumer.class);
        producer = mock(KafkaProducer.class);

        when(connector.getConsumerConnection(NORMAL)).thenReturn(consumerNorm);
        when(connector.getConsumerConnection(PRIORITY)).thenReturn(consumerPrio);
        when(connector.getProducerConnection()).thenReturn(producer);

        sut = new KafkaImpl(jsonUtils, connector, true);
    }

    @Test
    public void openThreeConnectionsOnConstruction() {
        verify(connector).getConsumerConnection(Lane.PRIORITY);
        verify(connector).getConsumerConnection(Lane.NORMAL);
        verify(connector).getProducerConnection();
    }

    @Test
    public void subscribesEndUpAtConnection_Class() {
        sut.subscribe("t", String.class, SOME_CB);
        verifySubscribe(consumerNorm, "t.out");
        verifySubscribe(consumerPrio, "t.priority.out");

        sut.subscribe("t2", String.class, SOME_CB);
        verifySubscribe(consumerNorm, 2, "t.out", "t2.out");
        verifySubscribe(consumerPrio, 2, "t.priority.out", "t2.priority.out");
    }

    @Test
    public void subscribesEndUpAtConnection_ClassErr() {
        sut.subscribe("t", String.class, SOME_CB, SOME_ERR_CB);
        verifySubscribe(consumerNorm, "t.out");
        verifySubscribe(consumerPrio, "t.priority.out");

        sut.subscribe("t2", String.class, SOME_CB, SOME_ERR_CB);
        verifySubscribe(consumerNorm, 2, "t.out", "t2.out");
        verifySubscribe(consumerPrio, 2, "t.priority.out", "t2.priority.out");
    }

    @Test
    public void subscribesEndUpAtConnection_TRef() {
        sut.subscribe("t", T_STRING, SOME_CB);
        verifySubscribe(consumerNorm, "t.out");
        verifySubscribe(consumerPrio, "t.priority.out");

        sut.subscribe("t2", T_STRING, SOME_CB, SOME_ERR_CB);
        verifySubscribe(consumerNorm, 2, "t.out", "t2.out");
        verifySubscribe(consumerPrio, 2, "t.priority.out", "t2.priority.out");
    }

    @Test
    public void subscribesEndUpAtConnection_TRefErr() {
        sut.subscribe("t", T_STRING, SOME_CB, SOME_ERR_CB);
        verifySubscribe(consumerNorm, "t.out");
        verifySubscribe(consumerPrio, "t.priority.out");

        sut.subscribe("t2", T_STRING, SOME_CB, SOME_ERR_CB);
        verifySubscribe(consumerNorm, 2, "t.out", "t2.out");
        verifySubscribe(consumerPrio, 2, "t.priority.out", "t2.priority.out");
    }

    @Test
    public void publishEndsUpAtConnection() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        registerSerialization("x", String.class);

        sut.publish("x", "t", PRIORITY);
        verify(producer).send(captor.capture());
        verify(producer).flush();

        var actual = captor.getValue().value();
        assertNotNull(actual);
        var expected = jsons.get("x");
        assertEquals(expected, actual);
    }

    @Test
    public void poll_propagatesToConsumers() {
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        sut.poll();
        verify(consumerPrio).poll(eq(Duration.ZERO));
        verify(consumerNorm).poll(eq(Duration.ZERO));
        verify(consumerPrio).commitSync();
        verify(consumerNorm).commitSync();
    }

    @Test
    public void poll_waitsAfterNoMessages() {
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        sut.poll();
        sut.poll();
        verify(consumerPrio).poll(eq(Duration.ZERO));
        verify(consumerPrio).poll(eq(Duration.ofSeconds(10)));
        verify(consumerNorm, times(2)).poll(eq(Duration.ZERO));
        verify(consumerPrio, times(2)).commitSync();
        verify(consumerNorm, times(2)).commitSync();
        verifyNoMoreInteractions(consumerNorm);
    }

    @Test
    public void poll_prioCausesHeartbeat() {
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(records("t.priority.out", "a"));
        sut.subscribe("t", String.class, SOME_CB);
        sut.poll();

        // regular poll
        verify(consumerPrio).poll(Duration.ZERO);
        verify(consumerPrio).commitSync();

        // heartbeat
        verifySubscribe(consumerNorm, "t.out");
        verify(consumerNorm).assignment();
        verify(consumerNorm).pause(anySet());
        verify(consumerNorm).poll(Duration.ZERO);
        verify(consumerNorm).resume(anySet());
        verifyNoMoreInteractions(consumerNorm);
    }

    @Test
    public void sendHeartbeat() {
        sut.sendHeartbeat();

        verify(consumerNorm).assignment();
        verify(consumerNorm).pause(anySet());
        verify(consumerNorm).poll(Duration.ZERO);
        verify(consumerNorm).resume(anySet());
        verifyNoMoreInteractions(consumerNorm);

        verify(consumerPrio).assignment();
        verify(consumerPrio).pause(anySet());
        verify(consumerPrio).poll(Duration.ZERO);
        verify(consumerPrio).resume(anySet());
        verifyNoMoreInteractions(consumerPrio);
    }

    @Test
    public void subIsTriggered() {
        registerSerialization("some_input", String.class);
        var wasCalled = new boolean[] { false };
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(records("t.priority.out", jsons.get("some_input")));
        sut.subscribe("t", String.class, (actual, l) -> {
            assertEquals(Lane.PRIORITY, l);
            assertEquals("some_input", actual);
            wasCalled[0] = true;
        });
        sut.poll();
        assertTrue(wasCalled[0]);
    }

    @Test
    public void errorCallbacksAreTriggered() {
        registerSerialization("some_input", String.class);
        registerSerialization("some_output", String.class);
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(records("t.priority.out", jsons.get("some_input")));

        var wasCalled = new boolean[] { false };
        var t = new IllegalArgumentException();
        sut.subscribe("t", String.class, (s, l) -> {
            throw t;
        }, (s, e) -> {
            assertTrue(e == t);
            assertEquals("some_input", s);
            wasCalled[0] = true;
            return "some_output";
        });
        sut.poll();
        assertTrue(wasCalled[0]);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture());
        var actual = captor.getValue();
        assertEquals(jsons.get("some_output"), actual.value());
        assertEquals("t.err", actual.topic());
    }

    @Test
    public void errorCallbackIgnoresNone() {
        registerSerialization("some_input", String.class);
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerPrio.poll(any(Duration.class))).thenReturn(records("t.priority.out", jsons.get("some_input")));
        sut.subscribe("t", String.class, (actual, l) -> {
            throw new RuntimeException();
        });
        sut.poll();
    }

    @Test
    public void commitIsAutoCalledWhenEnabled() {
        sut = new KafkaImpl(jsonUtils, connector, true);
        when(consumerPrio.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        sut.poll();
        verify(consumerPrio, times(1)).commitSync();
        verify(consumerNorm, times(1)).commitSync();
    }

    @Test
    public void commitIsNotCalledWhenDisabled() {
        sut = new KafkaImpl(jsonUtils, connector, false);
        when(consumerPrio.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        when(consumerNorm.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        sut.poll();
        verify(consumerPrio, times(0)).commitSync();
        verify(consumerNorm, times(0)).commitSync();
    }

    @Test
    public void commitCanBeManuallyCalledForBothConsumers() {
        sut = new KafkaImpl(jsonUtils, connector, true);
        sut.commit();
        verify(consumerPrio, times(1)).commitSync();
        verify(consumerNorm, times(1)).commitSync();
    }

    // utils

    private static ConsumerRecords<String, String> records(String topic, String... values) {
        var list = new LinkedList<ConsumerRecord<String, String>>();
        for (var value : values) {
            var r = new ConsumerRecord<String, String>(topic, 0, 0, null, value);
            list.add(r);
        }
        var records = new HashMap<TopicPartition, List<ConsumerRecord<String, String>>>();
        records.put(new TopicPartition("...", 0), list);
        return new ConsumerRecords<>(records);
    }

    @SuppressWarnings("unchecked")
    private <T> void registerSerialization(T o, Class<T> c) {
        var rnd = Double.toString(Math.random());
        jsons.put(o, rnd);
        when(jsonUtils.toJson(eq(o))).thenReturn(rnd);
        when(jsonUtils.fromJson(eq(rnd), any(Class.class))).thenReturn(o);
        when(jsonUtils.fromJson(eq(rnd), any(TRef.class))).thenReturn(o);
    }

    private static void verifySubscribe(KafkaConsumer<String, String> con, String... topics) {
        verifySubscribe(con, 1, topics);
    }

    private static void verifySubscribe(KafkaConsumer<String, String> con, int times, String... topics) {
        verify(con, times(times)).subscribe(eq(Set.of(topics)));
    }
}