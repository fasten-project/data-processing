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

import static eu.f4sten.infra.kafka.Lane.NORMAL;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import eu.f4sten.infra.impl.kafka.KafkaConnector;

public class MessageCollector {

    private final Set<String> internalTopics = new HashSet<String>();

    private final KafkaConsumer<String, String> con;

    @Inject
    public MessageCollector(KafkaConnector connector) {
        con = connector.getConsumerConnection(NORMAL);
        internalTopics.add("__consumer_offsets");
    }

    public Map<String, List<String>> collectAllMessages() {
        var publishedMsgs = new HashMap<String, List<String>>();

        var topics = con.listTopics();
        for (var topic : topics.keySet()) {
            if (internalTopics.contains(topic)) {
                continue;
            }

            System.out.printf("### Analyzing '%s' #######################\n", topic);

            System.out.printf("\nPartition info:\n");
            for (var part : topics.get(topic)) {
                System.out.printf(" - %s\n", part);
            }

            var isRebalancing = new boolean[] { true };
            con.subscribe(Set.of(topic), new ConsumerRebalanceListener() {

                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    System.out.println("Parts revoked");
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    System.out.println("Parts assigned");
                    isRebalancing[0] = false;
                }
            });
//            while (isRebalancing[0]) {
//                System.out.println("Waiting...");
//            }

//            con.poll(Duration.ZERO);
//            con.commitSync();
            con.seekToBeginning(con.assignment());

            System.out.printf("\nCollecting messages...\n[");
            var hadMessages = true;
            var msgs = msgsFor(publishedMsgs, topic);
            while (hadMessages) {
                var records = con.poll(Duration.ofSeconds(1));
                hadMessages = records.count() > 0;
                for (var r : records) {
                    System.out.printf(".");
                    msgs.add(r.value());
                }
            }
            System.out.println("]\n");
            con.commitSync();
        }

        return publishedMsgs;
    }

    private List<String> msgsFor(Map<String, List<String>> msgs, String topicName) {
        if (msgs.containsKey(topicName)) {
            return msgs.get(topicName);
        }
        var topic = new LinkedList<String>();
        msgs.put(topicName, topic);
        return topic;
    }
}