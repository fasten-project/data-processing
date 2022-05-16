/*
 * Copyright 2022 Delft University of Technology
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

package eu.f4sten.ingestartifactcompletion;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This is a tiny plug-in that mark normal artifacts that are FULLY completed/processed in the ingested_artifacts table.
 */
public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final IngestArtifactCompletionArgs args;
    private final Kafka kafka;
    private final DatabaseUtils db;

    private MavenId currMavenId;

    @Inject
    public Main(IngestArtifactCompletionArgs args, Kafka kafka, DatabaseUtils db) {
        this.args = args;
        this.kafka = kafka;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            AssertArgs.assertFor(args)//
                    .notNull(a -> a.kafkaIn, "kafka input topic"); //

            LOG.info("Subscribing to '{}'", args.kafkaIn);

            final var msgClass = new TRef<Message<Message<Message<Message
                                <MavenId, PomAnalysisResult>, Object>, Object>, Object>>() {
            };

            kafka.subscribe(args.kafkaIn, msgClass, (msg, l) -> {
                final var pomAnalysisResult = msg.input.input.input.payload;
                currMavenId = extractMavenIdFrom(pomAnalysisResult);
                LOG.info("Consuming next record ...");
                ingestCompletedArtifact();
            });
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    public void ingestCompletedArtifact() {
        db.markAsIngestedPackage(currMavenId.asCoordinate(), Lane.NORMAL);
        LOG.info("Marked normal artifact {} as completed", currMavenId.asCoordinate());
    }

    private MavenId extractMavenIdFrom(final PomAnalysisResult pomAnalysisResult) {
        final var mavenId = new MavenId();
        mavenId.groupId = pomAnalysisResult.groupId;
        mavenId.artifactId = pomAnalysisResult.artifactId;
        mavenId.version = pomAnalysisResult.version;
        return mavenId;
    }
}