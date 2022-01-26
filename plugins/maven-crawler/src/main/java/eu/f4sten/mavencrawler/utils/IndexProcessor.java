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

import static eu.f4sten.infra.kafka.Lane.NORMAL;

import java.util.Set;

import javax.inject.Inject;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;
import eu.f4sten.pomanalyzer.data.MavenId;

public class IndexProcessor {

    private final MavenCrawlerArgs args;
    private final LocalStore store;
    private final RemoteUtils utils;
    private final FileReader reader;
    private final Kafka kafka;

    @Inject
    public IndexProcessor(MavenCrawlerArgs args, LocalStore store, RemoteUtils utils, FileReader reader, Kafka kafka) {
        AssertArgs.assertFor(args) //
                .notNull(a -> a.kafkaOut, "kafka output");
        this.args = args;
        this.store = store;
        this.utils = utils;
        this.reader = reader;
        this.kafka = kafka;
    }

    public void tryProcessingNextIndices() {
        var nextIdx = store.getNextIndex();
        while (utils.exists(nextIdx)) {
            process(nextIdx);
            store.finish(nextIdx);
            nextIdx++;
        }
    }

    private void process(int idx) {
        System.out.printf("Working on index %d", idx);
        var file = utils.download(idx);
        Set<MavenId> artifacts = reader.readIndexFile(file);
        System.out.printf("Found %d artifacts", artifacts.size());
        for (var ma : artifacts) {
            kafka.publish(ma, args.kafkaOut, NORMAL);
        }
    }
}