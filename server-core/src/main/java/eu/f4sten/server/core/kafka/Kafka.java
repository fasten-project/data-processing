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
package eu.f4sten.server.core.kafka;

import java.util.function.BiConsumer;

import com.fasterxml.jackson.core.type.TypeReference;

public interface Kafka {

	<T> void subscribe(String topic, Class<T> messageType, BiConsumer<T, Lane> consumer);

	<T> void subscribe(String topic, TypeReference<T> messageType, BiConsumer<T, Lane> consumer);

	<T> void publish(T obj, String topic, Lane lane);

	void poll();
}