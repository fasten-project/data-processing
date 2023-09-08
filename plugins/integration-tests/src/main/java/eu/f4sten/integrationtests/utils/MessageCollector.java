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

import static dev.c0ps.franz.Lane.NORMAL;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_INSTANCE_ID_CONFIG;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import dev.c0ps.franz.KafkaConnector;
import jakarta.inject.Inject;

public class MessageCollector {

    private final Set<String> internalTopics = new HashSet<String>();

    private final KafkaConsumer<String, String> con;

    @Inject
    public MessageCollector(KafkaConnector connector) {
        String uniqueName = MessageCollector.class.getName() + new Date().getTime();
        var p = connector.getConsumerProperties(NORMAL);
        p.setProperty(GROUP_ID_CONFIG, uniqueName);
        p.remove(GROUP_INSTANCE_ID_CONFIG);
        con = new KafkaConsumer<>(p);

        internalTopics.add("__consumer_offsets");
    }

    public Map<String, List<String>> collectAllMessages() {
        var publishedMsgs = new HashMap<String, List<String>>();

        var topics = con.listTopics();
        for (var topic : topics.keySet()) {
            if (internalTopics.contains(topic)) {
                continue;
            }
            publishedMsgs.put(topic, collectMessages(topic));
        }

        return publishedMsgs;
    }

    private List<String> collectMessages(String topic) {
        con.subscribe(Set.of(topic));

        var msgs = new LinkedList<String>();
        Function<ConsumerRecords<String, String>, Boolean> registerMsgs = crs -> {
            for (var cr : crs) {
                msgs.add(cr.value());
            }
            return crs.count() > 0;
        };

        // first poll needs to wait for assignment/rebalance
        callUntilTrue(() -> {
            var crs = con.poll(Duration.ofMillis(100));
            return registerMsgs.apply(crs);
        });
        var hadMessages = true;
        while (hadMessages) {
            var crs = con.poll(Duration.ofMillis(100));
            hadMessages = registerMsgs.apply(crs);
        }

        return msgs;
    }

    private void callUntilTrue(BooleanSupplier s) {
        var count = 0;
        while (count++ < 10 && !s.getAsBoolean()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }
}