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
package eu.f4sten.depgraph;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.depgraph.data.Coordinates;
import eu.f4sten.depgraph.endpoints.DependencyGraphResolution;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.http.HttpServer;
import eu.f4sten.infra.kafka.Kafka;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final HttpServer server;
//    private final Kafka kafka;
//    private final Coordinates coords;

    @Inject
    public Main(HttpServer server, Kafka kafka, Coordinates coords) {
        this.server = server;
//        this.kafka = kafka;
//        this.coords = coords;
    }

    @Override
    public void run() {

//        server.register(Hello.class);
        server.register(DependencyGraphResolution.class);
        server.start();

//        kafka.subscribe(DefaultTopics.POM_ANALYZER, new TRef<Message<Void, PomAnalysisResult>>() {}, (m, l) -> {
//            var res = m.payload;
//            LOG.info("Adding coordinate {} ...", res);
//            coords.processed.add(res.toCoordinate());
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
//
//        while (!Thread.interrupted()) {
//            kafka.poll();
//        }
    }
}