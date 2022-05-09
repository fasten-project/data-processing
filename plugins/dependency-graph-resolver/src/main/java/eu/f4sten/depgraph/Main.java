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

import static eu.fasten.core.maven.resolution.MavenResolverIO.simplify;
import static eu.fasten.core.utils.MemoryUsageUtils.logMemoryUsage;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.http.HttpServer;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.DefaultTopics;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.infra.utils.IoUtils;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.maven.resolution.MavenResolverData;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final int NUM_TO_REPORT = 1000;

    // TODO this thresholds still need tweaking
    private static final int MIN_STORAGE_NUMBER = 10000;
    private static final long STORAGE_TIMEOUT = 5 * 60 * 1000; // 5min

    private final HttpServer server;
    private final Kafka kafka;

    private final MavenResolverData data;
    private final IoUtils io;

    private Set<Pom> poms = new HashSet<>();
    private long lastStoredAt = 0;
    private int numPomsAddedSinceLastStore = 0;

    @Inject
    public Main(HttpServer server, Kafka kafka, IoUtils io, MavenResolverData data1) {
        this.server = server;
        this.kafka = kafka;
        this.data = data1;
        this.io = io;
    }

    @Override
    public void run() {
        server.register(DependencyGraphResolution.class);
        server.start();

        LOG.info("Storage location for poms: {}", dbFile());

        initPomsAndDataContainers();

        kafka.subscribe(DefaultTopics.POM_ANALYZER, new TRef<Message<Void, Pom>>() {}, (m, l) -> {
            numPomsAddedSinceLastStore++;
            logProgress(m.payload);
            var pom = simplify(m.payload);
            poms.add(pom);

            data.add(pom);

            if (shouldStore()) {
                store();
            }
        });
        while (!Thread.interrupted()) {
            kafka.poll();
        }
    }

    private void initPomsAndDataContainers() {
        var f = dbFile();
        if (f.exists()) {
            time("Reading poms from file system", () -> {
                poms = io.readFromZip(dbFile(), new TRef<HashSet<Pom>>() {});
            });

            LOG.info("Registering {} poms with data containers", poms.size());
            var numAdded = 0;
            for (var pom : poms) {
                numAdded++;
                data.add(pom);
                if ((numAdded % NUM_TO_REPORT) == 0) {
                    LOG.info("Added {} more coordinates to data containers ...", NUM_TO_REPORT);
                }
            }
            time("Cleanup resolver data", () -> {
                data.removeOutdatedPomRegistrations();
            });
            LOG.info("Data containers ready");

            logMemoryUsage();

        } else {
            LOG.info("Starting to collect poms from scratch ...");
            poms = new HashSet<>();
        }
    }

    private boolean shouldStore() {
        var isOldEnough = now() - lastStoredAt > STORAGE_TIMEOUT;
        var hasAddedEnoughItems = numPomsAddedSinceLastStore >= MIN_STORAGE_NUMBER;
        return isOldEnough && hasAddedEnoughItems;
    }

    private void store() {
        var tmp = tmpFile();
        time("Storing poms to file system", () -> {
            LOG.info("Storing {} poms in total, {} new", poms.size(), numPomsAddedSinceLastStore);
            io.writeToZip(poms, tmp);
            // reduces likelihood of corruption as rename is MUCH faster than store
            io.move(tmp, dbFile());
            kafka.commit();

            numPomsAddedSinceLastStore = 0;
            lastStoredAt = now();
        });

        LOG.info("Removing outdated Pom registrations ...");
        data.removeOutdatedPomRegistrations();
    }

    private File tmpFile() {
        var f = Paths.get(io.getBaseFolder().getAbsolutePath(), "mvn_depgraph", "poms.json-tmp").toFile();
        return f;
    }

    private File dbFile() {
        var f = Paths.get(io.getBaseFolder().getAbsolutePath(), "mvn_depgraph", "poms.zip").toFile();
        return f;
    }

    private void logProgress(Pom pom) {
        LOG.debug("Adding coordinate {} ...", pom.toCoordinate());
        var wasSomethingAdded = numPomsAddedSinceLastStore > 0;
        var isHittingProgressThreshold = (numPomsAddedSinceLastStore % NUM_TO_REPORT) == 0;
        if (wasSomethingAdded && isHittingProgressThreshold) {
            LOG.info("Added {} more coordinates through Kafka ...", NUM_TO_REPORT);
            logMemoryUsage();
        }
    }

    private static void time(String activity, Runnable r) {
        LOG.info("{} ...", activity);
        var start = now();
        r.run();
        var end = now();
        LOG.info("{} took {} ms", activity, end - start);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}