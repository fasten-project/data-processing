/*
 * Copyright 2022 Software Improvement Group
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
package eu.f4sten.sourcesprovider;

import com.google.inject.Inject;
import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.kafka.Message;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourcesProvider implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(SourcesProvider.class);
    private final Kafka kafka;
    private final SourcesProviderArgs args;

    @Inject
    public SourcesProvider(Kafka kafka, SourcesProviderArgs args) {
        this.kafka = kafka;
        this.args = args;
        AssertArgs.assertFor(args)
            .notNull(a -> a.kafkaIn, "kafka in")
            .notNull(a -> a.kafkaOut, "kafka out");
    }

    @Override
    public void run() {
        try {
            LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);
            kafka.subscribe(args.kafkaIn, Message.class, this::consume);
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    private void consume(Message<JSONObject, JSONObject> message, Lane lane) {
        var input = message.input;
        var payload = message.payload;

        LOG.info("Consuming next {} record {} ...", lane, input.toString());

        kafka.publish(payload, args.kafkaOut, lane);
    }
}