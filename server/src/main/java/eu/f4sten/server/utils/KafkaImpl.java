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
package eu.f4sten.server.utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.f4sten.server.ServerArgs;
import eu.f4sten.server.core.Asserts;
import eu.f4sten.server.core.json.JsonUtils;
import eu.f4sten.server.core.kafka.Kafka;
import eu.f4sten.server.core.kafka.Lane;

public class KafkaImpl implements Kafka {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaImpl.class);
	private static final Duration POLL_TIMEOUT_PRIO = Duration.ofSeconds(10);

	private final JsonUtils jsonUtils;

	private final KafkaConsumer<String, String> connNorm;
	private final KafkaConsumer<String, String> connPrio;
	private final KafkaProducer<String, String> producer;

	private final Set<String> subsNorm = new HashSet<>();
	private final Set<String> subsPrio = new HashSet<>();
	private final Map<String, Set<Callback<?>>> callbacks = new HashMap<>();

	private boolean hadMessages = true;

	@Inject
	public KafkaImpl(JsonUtils jsonUtils, KafkaConnector connector, ServerArgs args) {
		this.jsonUtils = jsonUtils;
		connNorm = connector.getConsumerConnection(getFullInstanceId(args, Lane.NORMAL));
		connPrio = connector.getConsumerConnection(getFullInstanceId(args, Lane.PRIORITY));
		producer = connector.getProducerConnection();
	}

	private static String getFullInstanceId(ServerArgs args, Lane lane) {
		if (args.instanceId == null) {
			return null;
		}
		Asserts.that(!args.instanceId.isEmpty(), "instance id must be null or non-empty");
		return args.plugin + "-" + args.instanceId + "." + lane;
	}

	@Override
	public <T> void subscribe(String topic, Class<T> messageType, BiConsumer<T, Lane> callback) {
		subscribe(topic, new TypeReference<T>() {}, callback);
	}

	@Override
	public <T> void subscribe(String topic, TypeReference<T> messageType, BiConsumer<T, Lane> callback) {
		for (var lane : Lane.values()) {
			getCallbacks(topic, lane).add(new Callback<T>(messageType, callback));
		}
		subsNorm.add(combine(topic, Lane.NORMAL));
		connNorm.subscribe(subsNorm);
		subsPrio.add(combine(topic, Lane.PRIORITY));
		connPrio.subscribe(subsPrio);
	}

	private Set<Callback<?>> getCallbacks(String topic, Lane lane) {
		var key = combine(topic, lane);
		Set<Callback<?>> vals;
		if (!callbacks.containsKey(key)) {
			vals = new HashSet<Callback<?>>();
			callbacks.put(key, vals);
		} else {
			vals = callbacks.get(key);
		}
		return vals;
	}

	@Override
	public <T> void publish(T obj, String topic, Lane lane) {
		String json = jsonUtils.toJson(obj);
		var combinedTopic = combine(topic, lane);
		var record = new ProducerRecord<String, String>(combinedTopic, json);
		producer.send(record);
		producer.flush();
	}

	@Override
	public void poll() {
		// don't wait if any lane had messages, otherwise, only wait in PRIO
		var timeout = hadMessages ? Duration.ZERO : POLL_TIMEOUT_PRIO;
		if (process(connPrio, Lane.PRIORITY, timeout)) {
			// make sure the session does not time out
			sendHeartBeat(connNorm);
		} else {
			process(connNorm, Lane.NORMAL, Duration.ZERO);
		}
	}

	private boolean process(KafkaConsumer<String, String> con, Lane lane, Duration timeout) {
		hadMessages = false;
		for (var r : con.poll(timeout)) {
			hadMessages = true;
			var json = r.value();
			Set<Callback<?>> cbs = callbacks.get(r.topic());
			for (var cb : cbs) {
				cb.exec(json, lane);
			}
		}
		con.commitSync();
		return hadMessages;
	}

	private static String combine(String topic, Lane lane) {
		return topic + "." + lane;
	}

	private static void sendHeartBeat(KafkaConsumer<?, ?> c) {
		// See https://stackoverflow.com/a/43722731
		var partitions = c.assignment();
		c.pause(partitions);
		c.poll(Duration.ZERO);
		c.resume(partitions);
	}

	private class Callback<T> {

		private final TypeReference<T> messageType;
		private final BiConsumer<T, Lane> callback;

		private Callback(TypeReference<T> messageType, BiConsumer<T, Lane> callback) {
			this.messageType = messageType;
			this.callback = callback;
		}

		private void exec(String json, Lane lane) {
			T obj = jsonUtils.fromJson(json, messageType);
			callback.accept(obj, lane);
		}
	}
}