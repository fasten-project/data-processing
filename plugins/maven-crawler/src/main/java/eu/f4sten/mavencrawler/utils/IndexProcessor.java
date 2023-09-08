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
package eu.f4sten.mavencrawler.utils;

import static dev.c0ps.franz.Lane.NORMAL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.franz.Kafka;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;
import jakarta.inject.Inject;

public class IndexProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(IndexProcessor.class);

    private final MavenCrawlerArgs args;
    private final LocalStore store;
    private final EasyIndexClient utils;
    private final Kafka kafka;

    @Inject
    public IndexProcessor(MavenCrawlerArgs args, LocalStore store, EasyIndexClient utils, Kafka kafka) {
        AssertArgs.assertFor(args) //
                .notNull(a -> a.kafkaOut, "kafka output");
        this.args = args;
        this.store = store;
        this.utils = utils;
        this.kafka = kafka;
    }

    public void tryProcessingNextIndices() {
        var nextIdx = store.getNextIndex();
        LOG.info("Processing index {} ...", nextIdx);
        while (utils.exists(nextIdx)) {
            process(nextIdx);
            store.finish(nextIdx);
            nextIdx++;
        }
        LOG.info("Index {} cannot be found", nextIdx);
    }

    private void process(int idx) {
        var artifacts = utils.get(idx);
        LOG.info("Publishing {} coordinates ...", artifacts.size());
        for (var ma : artifacts) {
            LOG.debug("Publishing: {}:{}:{}", ma.groupId, ma.artifactId, ma.version);
            kafka.publish(ma, args.kafkaOut, NORMAL);
        }
    }
}