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
package eu.f4sten.pomanalyzer;

import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static eu.f4sten.infra.kafka.Lane.PRIORITY;
import static java.lang.String.format;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.pomanalyzer.data.ResolutionResult;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import eu.f4sten.pomanalyzer.utils.EffectiveModelBuilder;
import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;
import eu.f4sten.pomanalyzer.utils.PackagingFixer;
import eu.f4sten.pomanalyzer.utils.PomExtractor;
import eu.f4sten.pomanalyzer.utils.Resolver;
import eu.fasten.core.maven.utils.MavenUtilities;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final int EXEC_DELAY_MS = 1000;

    private final MavenRepositoryUtils repo;
    private final EffectiveModelBuilder modelBuilder;
    private final PomExtractor extractor;
    private final DatabaseUtils db;
    private final Resolver resolver;
    private final Kafka kafka;
    private final PomAnalyzerArgs args;
    private final MessageGenerator msgs;
    private final PackagingFixer fixer;

    private final Date startedAt = new Date();
    private final Set<String> ingested = new HashSet<>();

    private MavenId curId;

    @Inject
    public Main(MavenRepositoryUtils repo, EffectiveModelBuilder modelBuilder, PomExtractor extractor, DatabaseUtils db,
            Resolver resolver, Kafka kafka, PomAnalyzerArgs args, MessageGenerator msgs, PackagingFixer fixer) {
        this.repo = repo;
        this.modelBuilder = modelBuilder;
        this.extractor = extractor;
        this.db = db;
        this.resolver = resolver;
        this.kafka = kafka;
        this.args = args;
        this.msgs = msgs;
        this.fixer = fixer;
    }

    @Override
    public void run() {
        AssertArgs.assertFor(args)//
                .notNull(a -> a.kafkaIn, "kafka input topic") //
                .notNull(a -> a.kafkaOut, "kafka output topic");

        LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);
        kafka.subscribe(args.kafkaIn, MavenId.class, (id, lane) -> {
            curId = id;

            LOG.info("Consuming next record {} ...", id.asCoordinate());
            LOG.debug("{}", id);
            var artifact = bootstrapFirstResolutionResultFromInput(id);
            runAndCatch(artifact, lane, () -> {
                resolver.resolveIfNotExisting(artifact);
                process(artifact, lane);
            });
        });
        while (true) {
            LOG.debug("Polling ...");
            kafka.poll();
        }
    }

    private void runAndCatch(ResolutionResult artifact, Lane lane, Runnable r) {
        try {
            if (shouldSkip(artifact.coordinate, lane)) {
                LOG.info("Coordinate {} has already been ingested. Skipping.", artifact.coordinate);
                return;
            }
            r.run();
        } catch (Exception e) {
            // if execution crashes, prevent re-try for both lanes
            memMarkAsIngestedPackage(artifact.coordinate, NORMAL);
            memMarkAsIngestedPackage(artifact.coordinate, PRIORITY);

            LOG.warn("Execution failed for (original) input: {}", curId, e);

            boolean isRuntimeExceptionAndNoSubtype = RuntimeException.class.equals(e.getClass());
            boolean isWrapped = isRuntimeExceptionAndNoSubtype && e.getCause() != null;

            var msg = msgs.getErr(curId, isWrapped ? e.getCause() : e);
            kafka.publish(msg, args.kafkaOut, Lane.ERROR);
        }
    }

    private static ResolutionResult bootstrapFirstResolutionResultFromInput(MavenId id) {
        var artifactRepository = MavenUtilities.MAVEN_CENTRAL_REPO;
        if (id.artifactRepository != null) {
            var val = id.artifactRepository.strip();
            if (!val.isEmpty()) {
                artifactRepository = val;
            }
        }
        return new ResolutionResult(id.asCoordinate(), artifactRepository);
    }

    private void process(ResolutionResult artifact, Lane lane) {
        var duration = Duration.between(startedAt.toInstant(), new Date().toInstant());
        var msg = "Processing {} ... (dependency of: {}, started at: {}, duration: {})";
        LOG.info(msg, artifact.coordinate, curId.asCoordinate(), startedAt, duration);

        delayExecutionToPreventThrottling();

        var consumedAt = new Date();
        kafka.sendHeartbeat();

        // merge pom with all its parents and resolve properties
        var m = modelBuilder.buildEffectiveModel(artifact.localPomFile);

        // extract details
        var result = extractor.process(m);
        result.artifactRepository = artifact.artifactRepository;
        // packaging often bogus, check and possibly fix
        result.packagingType = fixer.checkPackage(result);
        result.sourcesUrl = repo.getSourceUrlIfExisting(result);
        result.releaseDate = repo.getReleaseDate(result);

        store(result, lane, consumedAt);

        // for performance (and to prevent cycles), remember visited coordinates in-mem
        memMarkAsIngestedPackage(artifact.coordinate, lane);
        memMarkAsIngestedPackage(result.toCoordinate(), lane);

        // resolve dependencies to
        // 1) have dependencies
        // 2) identify artifact sources
        // 3) make sure all dependencies exist in local .m2 folder
        var deps = resolver.resolveDependenciesFromPom(artifact.localPomFile, artifact.artifactRepository);

        // resolution can be different for dependencies, so 'process' them independently
        deps.forEach(dep -> {
            runAndCatch(dep, lane, () -> {
                process(dep, lane);
            });
        });

        // to stay crash resilient, only mark in DB once all deps have been processed
        moveIngestionMarkFromMemToDb(artifact.coordinate, lane);
        moveIngestionMarkFromMemToDb(result.toCoordinate(), lane);
    }

    private void store(PomAnalysisResult result, Lane lane, Date consumedAt) {
        LOG.debug("Finished: {}", result);
        if (existsInDatabase(result.toCoordinate(), lane)) {
            // reduce the opportunity for race-conditions by re-checking before storing
            return;
        }
        db.save(result);
        var m = msgs.getStd(result);
        m.consumedAt = consumedAt;
        kafka.publish(m, args.kafkaOut, lane);
    }

    private boolean shouldSkip(String coordinate, Lane lane) {
        return existsInMemory(coordinate, lane) || existsInDatabase(coordinate, lane);
    }

    private void memMarkAsIngestedPackage(String coord, Lane lane) {
        ingested.add(toKey(coord, lane));
    }

    private void moveIngestionMarkFromMemToDb(String coord, Lane lane) {
        db.markAsIngestedPackage(coord, lane);
        ingested.remove(toKey(coord, lane));
    }

    private static String toKey(String coordinate, Lane lane) {
        return format("%s-%s", coordinate, lane);
    }

    private boolean existsInMemory(String coordinate, Lane lane) {
        return lane == NORMAL
                ? ingested.contains(toKey(coordinate, NORMAL)) || ingested.contains(toKey(coordinate, PRIORITY))
                : ingested.contains(toKey(coordinate, lane));
    }

    private boolean existsInDatabase(String coordinate, Lane lane) {
        return lane == NORMAL
                ? db.hasPackageBeenIngested(coordinate, NORMAL) || db.hasPackageBeenIngested(coordinate, PRIORITY)
                : db.hasPackageBeenIngested(coordinate, lane);
    }

    private static void delayExecutionToPreventThrottling() {
        try {
            Thread.sleep(EXEC_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}