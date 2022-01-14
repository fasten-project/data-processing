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

import static eu.f4sten.server.core.kafka.Lane.ERROR;
import static eu.f4sten.server.core.kafka.Lane.NORMAL;
import static eu.f4sten.server.core.kafka.Lane.PRIORITY;
import static java.time.Duration.ZERO;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import eu.f4sten.server.ServerArgs;
import eu.f4sten.server.core.json.JsonUtils;
import eu.f4sten.server.core.json.TRef;
import eu.f4sten.server.core.kafka.Kafka;
import eu.f4sten.server.core.kafka.Lane;

public class KafkaImpl implements Kafka {

	private static final Duration POLL_TIMEOUT_PRIO = Duration.ofSeconds(10);
	private static final Object NONE = new Object();

	private final JsonUtils jsonUtils;

	private final KafkaConsumer<String, String> connNorm;
	private final KafkaConsumer<String, String> connPrio;
	private final KafkaProducer<String, String> producer;

	// keep track of subscriptions to allow incremental calls
	private final Set<String> subsNorm = new HashSet<>();
	private final Set<String> subsPrio = new HashSet<>();

	private final Map<String, String> baseTopics = new HashMap<>();
	private final Map<String, Set<Callback<?>>> callbacks = new HashMap<>();

	private boolean hadMessages = true;

	@Inject
	public KafkaImpl(JsonUtils jsonUtils, KafkaConnector connector, ServerArgs args) {
		this.jsonUtils = jsonUtils;
		connNorm = connector.getConsumerConnection(NORMAL);
		connPrio = connector.getConsumerConnection(PRIORITY);
		producer = connector.getProducerConnection();
	}

	@Override
	public <T> void subscribe(String topic, Class<T> type, BiConsumer<T, Lane> callback) {
		subscribe(topic, new TRef<T>() {}, callback);
	}

	@Override
	public <T> void subscribe(String topic, Class<T> type, BiConsumer<T, Lane> callback,
			BiFunction<T, Throwable, ?> errors) {
		subscribe(topic, new TRef<T>() {}, callback, errors);
	}

	@Override
	public <T> void subscribe(String topic, TRef<T> typeRef, BiConsumer<T, Lane> callback) {
		subscribe(topic, new TRef<T>() {}, callback, (x, y) -> NONE);
	}

	@Override
	public <T> void subscribe(String topic, TRef<T> typeRef, BiConsumer<T, Lane> callback,
			BiFunction<T, Throwable, ?> errors) {
		for (var lane : Lane.values()) {
			getCallbacks(topic, lane).add(new Callback<T>(typeRef, callback, errors));
		}
		subsNorm.add(combine(topic, NORMAL));
		connNorm.subscribe(subsNorm);
		subsPrio.add(combine(topic, PRIORITY));
		connPrio.subscribe(subsPrio);
	}

	private Set<Callback<?>> getCallbacks(String baseTopic, Lane lane) {
		var combinedTopic = combine(baseTopic, lane);
		baseTopics.put(combinedTopic, baseTopic);

		Set<Callback<?>> vals;
		if (!callbacks.containsKey(combinedTopic)) {
			vals = new HashSet<Callback<?>>();
			callbacks.put(combinedTopic, vals);
		} else {
			vals = callbacks.get(combinedTopic);
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
		// don't wait if no lane had messages, otherwise, only wait in PRIO
		var timeout = hadMessages ? ZERO : POLL_TIMEOUT_PRIO;
		if (process(connPrio, Lane.PRIORITY, timeout)) {
			// make sure the session does not time out
			sendHeartBeat(connNorm);
		} else {
			process(connNorm, NORMAL, ZERO);
		}
	}

	private boolean process(KafkaConsumer<String, String> con, Lane lane, Duration timeout) {
		hadMessages = false;
		for (var r : con.poll(timeout)) {
			hadMessages = true;
			var json = r.value();
			Set<Callback<?>> cbs = callbacks.get(r.topic());
			for (var cb : cbs) {
				cb.exec(r.topic(), json, lane);
			}
		}
		con.commitSync();
		return hadMessages;
	}

	private static String combine(String topic, Lane lane) {
		return topic + "." + lane.extension;
	}

	private static void sendHeartBeat(KafkaConsumer<?, ?> c) {
		// See https://stackoverflow.com/a/43722731
		var partitions = c.assignment();
		c.pause(partitions);
		c.poll(ZERO);
		c.resume(partitions);
	}

	private class Callback<T> {

		private final TRef<T> messageType;
		private final BiConsumer<T, Lane> callback;
		private final BiFunction<T, Throwable, ?> errors;

		private Callback(TRef<T> messageType, BiConsumer<T, Lane> callback, BiFunction<T, Throwable, ?> errors) {
			this.messageType = messageType;
			this.callback = callback;
			this.errors = errors;
		}

		private void exec(String combinedTopic, String json, Lane lane) {
			T obj = null;
			try {
				obj = jsonUtils.fromJson(json, messageType);
				callback.accept(obj, lane);
			} catch (Throwable t) {
				var errTopic = combine(baseTopics.get(combinedTopic), ERROR);
				var err = errors.apply(obj, t);
				// check instance equality!
				if (err != NONE) {
					publish(err, errTopic, ERROR);
				}
			}
		}
	}
}