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

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.franz.Kafka;
import dev.c0ps.franz.Lane;
import eu.f4sten.sourcesprovider.utils.PayloadParsing;
import jakarta.inject.Inject;

public class Main implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private final Kafka kafka;
    private final SourcesProviderArgs args;
    private final PayloadParsing payloadParser;

    @Inject
    public Main(Kafka kafka, SourcesProviderArgs args, PayloadParsing payloadParser) {
        this.kafka = kafka;
        this.args = args;
        this.payloadParser = payloadParser;
        AssertArgs.assertFor(args).notNull(a -> a.kafkaIn, "kafka in").notNull(a -> a.kafkaOut, "kafka out");
    }

    @Override
    public void run() {
        try {
            LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);
            kafka.subscribe(args.kafkaIn, LinkedHashMap.class, this::consume);
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    private void consume(LinkedHashMap<String, String> message, Lane lane) {
        var json = new JSONObject(message);
        LOG.info("Consuming next {} record {} ...", lane, json);
        var sourcePayload = payloadParser.findSourcePayload(json);
        if (sourcePayload != null) {
            kafka.publish(sourcePayload, args.kafkaOut, lane);
        } else {
            var errorMessage = new JSONObject();
            errorMessage.put("Could not parse source payload for input", json);
            kafka.publish(errorMessage, args.kafkaOut, Lane.ERROR);
            LOG.error("Could not parse source payload on {} for record {} ...", lane, json);
        }
    }
}