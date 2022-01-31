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
package eu.f4sten.integrationtests.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Function;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Lane;

public class Messages {

    private final MessageCollector collector;
    private final JsonUtils json;

    protected Map<String, List<String>> msgsByTopic;

    @Inject
    public Messages(MessageCollector collector, JsonUtils json) {
        this.collector = collector;
        this.json = json;
    }

    public void collectAll() {
        msgsByTopic = collector.collectAllMessages();
    }

    public <T> List<T> get(String baseTopic, Lane lane, Class<T> type) {
        return get(baseTopic, lane, (String m) -> {
            return json.fromJson(m, type);
        });
    }

    public <T> List<T> get(String baseTopic, Lane lane, TRef<T> typeRef) {
        return get(baseTopic, lane, (String m) -> {
            return json.fromJson(m, typeRef);
        });
    }

    public <T> List<T> get(String baseTopic, Lane lane, Function<String, T> fun) {
        var topic = topic(baseTopic, lane);
        if (!msgsByTopic.containsKey(topic)) {
            return List.of();
        }
        return msgsByTopic.get(topic).stream() //
                .map(m -> fun.apply(m)) //
                .collect(Collectors.toList());
    }

    public String topic(String baseTopic, Lane normal) {
        return String.format("%s.%s", baseTopic, normal.extension);
    }
}