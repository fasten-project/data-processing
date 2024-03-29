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
package eu.f4sten.ingestedartifactcompletion;

import static eu.f4sten.pomanalyzer.data.Coordinates.toCoordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.franz.Kafka;
import dev.c0ps.franz.Lane;
import dev.c0ps.io.TRef;
import dev.c0ps.maven.data.Pom;
import dev.c0ps.maveneasyindex.Artifact;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import jakarta.inject.Inject;

public class Main implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final IngestedArtifactCompletionArgs args;
    private final Kafka kafka;
    private final DatabaseUtils db;

    @Inject
    public Main(IngestedArtifactCompletionArgs args, Kafka kafka, DatabaseUtils db) {
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

            final var msgClass = new TRef<Message<Message<Message<Message<Artifact, Pom>, Object>, Object>, Object>>() {};

            kafka.subscribe(args.kafkaIn, msgClass, (msg, l) -> {
                final var pom = msg.input.input.input.payload;

                if (l == Lane.PRIORITY) {
                    LOG.info("No processing required for package on priority lane ... ({})", pom.toCoordinate());
                    return;
                }

                LOG.info("Marking package as fully ingested ... ({})", pom.toCoordinate());
                var mavenId = extractMavenId(pom);
                // without packaging (g:a:?:v)
                db.markAsIngestedPackage(toCoordinate(mavenId), Lane.PRIORITY);
                // with packaging (g:a:jar:v)
                db.markAsIngestedPackage(toCoordinate(pom), Lane.PRIORITY);
            });
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    private MavenId extractMavenId(final Pom pom) {
        var id = new MavenId();
        id.groupId = pom.groupId;
        id.artifactId = pom.artifactId;
        id.version = pom.version;
        return id;
    }
}