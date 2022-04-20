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

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import eu.fasten.core.maven.data.Dependency;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.maven.resolution.MavenDependencyData;
import eu.fasten.core.maven.resolution.MavenDependentsData;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    // TODO this thresholds still need tweaking
    private static final int MIN_STORAGE_NUMBER = 1000; // 1000;
    private static final long STORAGE_TIMEOUT = 1 * 60 * 1000; // 1min

    private final HttpServer server;
    private final Kafka kafka;

    private final MavenDependencyData data1;
    private final MavenDependentsData data2;
    private final IoUtils io;

    private Set<Pom> poms = new HashSet<>();
    private long lastStoredAt = 0;
    private int numPomsAddedSinceLastStore = 0;

    @Inject
    public Main(HttpServer server, Kafka kafka, IoUtils io, MavenDependencyData data1, MavenDependentsData data2) {
        this.server = server;
        this.kafka = kafka;
        this.data1 = data1;
        this.data2 = data2;
        this.io = io;
    }

    @Override
    public void run() {

        // TODO this class still needs testing!

        server.register(DependencyGraphResolution.class);
        server.start();

        LOG.info("Storage location for poms: {}", dbFile());

        initPoms();

        kafka.subscribe(DefaultTopics.POM_ANALYZER, new TRef<Message<Void, Pom>>() {}, (m, l) -> {
            LOG.info("Adding coordinate {} ...", m.payload.toCoordinate());
            var pom = simplify(m.payload);
            poms.add(pom);
            numPomsAddedSinceLastStore++;

            register(pom);

            if (shouldStore()) {
                store();
            }
        });
        while (!Thread.interrupted()) {
            kafka.poll();
        }
    }

    private void register(Pom pom) {
        data1.add(pom);
        data2.add(pom);
    }

    private void initPoms() {
        var f = dbFile();
        if (f.exists()) {
            time("Reading poms from file system", () -> {
                poms = io.readFromZip(dbFile(), new TRef<Set<Pom>>() {});
            });

            LOG.info("Registering {} poms with data containers", poms.size());
            for (var pom : poms) {
                register(pom);
            }
            LOG.info("Data containers ready");

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
    }

    private File tmpFile() {
        var f = Paths.get(io.getBaseFolder().getAbsolutePath(), "mvn_depgraph", "poms.json-tmp").toFile();
        return f;
    }

    private File dbFile() {
        var f = Paths.get(io.getBaseFolder().getAbsolutePath(), "mvn_depgraph", "poms.zip").toFile();
        return f;
    }

    private static Pom simplify(Pom pom) {
        pom.forge = null;
        pom.repoUrl = null;
        pom.sourcesUrl = null;
        pom.artifactRepository = null;
        pom.packagingType = null;
        pom.parentCoordinate = null;
        pom.projectName = null;
        pom.commitTag = null;

        for (var d : new LinkedHashSet<>(pom.dependencies)) {
            // re-adding necessary on hash change
            pom.dependencies.remove(d);
            pom.dependencies.add(simplify(d));
        }

        for (var d : new LinkedHashSet<>(pom.dependencyManagement)) {
            // re-adding necessary on hash change
            pom.dependencyManagement.remove(d);
            pom.dependencyManagement.add(simplify(d));
        }

        return pom;
    }

    private static Dependency simplify(Dependency d) {
        d.setPackagingType(null);
        d.setClassifier(null);
        return d;
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