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
package eu.f4sten.swhinserter;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.sourcesprovider.data.SourcePayload;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final SwhInserterArgs args;
    private final Kafka kafka;
    private final DatabaseUtils db;
    private final IoUtils io;

    private final SwhHashCalculator calc;

    @Inject
    public Main(SwhInserterArgs args, Kafka kafka, DatabaseUtils db, IoUtils io, SwhHashCalculator calc) {
        this.args = args;
        this.kafka = kafka;
        this.db = db;
        this.io = io;
        this.calc = calc;
    }

    @Override
    public void run() {

        try {
            AssertArgs.assertFor(args)//
                    .notNull(a -> a.kafkaIn, "kafka input topic"); //

            LOG.info("Subscribing to '{}'", args.kafkaIn);
            kafka.subscribe(args.kafkaIn, SourcePayload.class, this::consume);
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    public void consume(SourcePayload payload, Lane lane) {
        LOG.info("Consuming next {} record ...", lane);
        var pkgName = payload.getProduct();
        var ver = payload.getVersion();

        var basePath = getBasePath(payload.getForge(), pkgName, ver);

        var pkgVerID = db.getPkgVersionID(pkgName, ver);
        var paths = db.getFilePaths4PkgVersion(pkgVerID);

        paths.forEach(path -> {
            try {
                var hash = calc.calc(basePath, path);
                db.addFileHash(pkgVerID, path, hash);
                LOG.info("Added file hash for {}", path);
            } catch (UncheckedIOException | IllegalStateException e) {
                LOG.error("Unable to process '{}' ({}: {})", path, e.getClass(), e.getMessage());
            }
        });
    }

    private File getBasePath(String forge, String pkgName, String version) {
        String[] ga = pkgName.split(":");
        var groupID = ga[0];
        var artifactID = ga[1];
        var baseDir = io.getBaseFolder().getPath();
        var firstChar = Character.toString(groupID.charAt(0));
        var basePath = Path.of(baseDir, "sources", forge, firstChar, groupID, artifactID, version).toFile();
        return basePath;
    }
}