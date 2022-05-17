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

import static java.lang.String.format;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import eu.f4sten.pomanalyzer.exceptions.ExecutionTimeoutError;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import eu.f4sten.pomanalyzer.utils.EffectiveModelBuilder;
import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;
import eu.f4sten.pomanalyzer.utils.PackagingFixer;
import eu.f4sten.pomanalyzer.utils.PomExtractor;
import eu.f4sten.pomanalyzer.utils.ProgressTracker;
import eu.f4sten.pomanalyzer.utils.Resolver;
import eu.fasten.core.maven.utils.MavenUtilities;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private static final int EXEC_DELAY_MS = 1000;
    private static final int EXECUTION_TIMEOUT_MS = 1000 * 60 * 10; // 10min

    private final ProgressTracker tracker;
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

    @Inject
    public Main(ProgressTracker tracker, MavenRepositoryUtils repo, EffectiveModelBuilder modelBuilder,
            PomExtractor extractor, DatabaseUtils db, Resolver resolver, Kafka kafka, PomAnalyzerArgs args,
            MessageGenerator msgs, PackagingFixer fixer) {
        this.tracker = tracker;
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
        try {
            AssertArgs.assertFor(args)//
                    .notNull(a -> a.kafkaIn, "kafka input topic") //
                    .notNull(a -> a.kafkaOut, "kafka output topic");

            LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);
            kafka.subscribe(args.kafkaIn, MavenId.class, this::consumeWithTimeout);
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    private void consumeWithTimeout(MavenId id, Lane lane) {
        LOG.info("Consuming next {} record {} @ {} ...", lane, id.asCoordinate(), id.artifactRepository);
        var artifact = bootstrapFirstResolutionResultFromInput(id);

        var future = EXEC.submit(() -> {
            tracker.startNextOriginal(id);
            tracker.registerRetry(artifact, lane);
            runAndCatch(artifact, lane);
            tracker.pruneRetries(artifact, lane);
        });

        try {
            future.get(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            var msg = "Execution timeout after %dms: %s (%s)";
            throw new ExecutionTimeoutError(
                    format(msg, EXECUTION_TIMEOUT_MS, artifact.coordinate, artifact.artifactRepository));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // this should not happen
            var cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
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

    private void runAndCatch(ResolutionResult artifact, Lane lane) {
        try {
            if (tracker.shouldSkip(artifact, lane)) {
                LOG.info("Skipping coordinate {}", artifact.coordinate);
                return;
            }
            process(artifact, lane);
        } catch (Exception e) {
            tracker.executionCrash(artifact, lane);

            LOG.warn("Execution failed for {} (original: {})", artifact.coordinate,
                    tracker.getCurrentOriginal().asCoordinate(), e);

            boolean isRuntimeExceptionAndNoSubtype = RuntimeException.class.equals(e.getClass());
            boolean isWrapped = isRuntimeExceptionAndNoSubtype && e.getCause() != null;

            var msg = msgs.getErr(tracker.getCurrentOriginal(), isWrapped ? e.getCause() : e);
            kafka.publish(msg, args.kafkaOut, Lane.ERROR);
        }
    }

    private void process(ResolutionResult artifact, Lane lane) {
        var duration = Duration.between(startedAt.toInstant(), new Date().toInstant());
        var msg = "Processing {} ... (dependency of: {}, started at: {}, running for: {})";
        LOG.info(msg, artifact.coordinate, tracker.getCurrentOriginal().asCoordinate(), startedAt, duration);
        delayExecutionToPreventThrottling();

        var consumedAt = new Date();
        kafka.sendHeartbeat();
        resolver.resolveIfNotExisting(artifact);

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
        tracker.markCompletionInMem(artifact.coordinate, lane);
        tracker.markCompletionInMem(result.toCoordinate(), lane);

        // resolve dependencies to
        // 1) have dependencies
        // 2) identify artifact sources
        // 3) make sure all dependencies exist in local .m2 folder
        var deps = resolver.resolveDependenciesFromPom(artifact.localPomFile, artifact.artifactRepository);

        // resolution can be different for dependencies, so 'process' them independently
        deps.forEach(dep -> {
            runAndCatch(dep, lane);
        });

        // to stay crash resilient, only mark in DB once all deps have been processed
        if (lane == Lane.PRIORITY) {
            tracker.markCompletionInDb(artifact.coordinate, lane);
            tracker.markCompletionInDb(result.toCoordinate(), lane);
        }
    }

    private void store(PomAnalysisResult result, Lane lane, Date consumedAt) {
        LOG.info("Storing results for {} ...", result.toCoordinate());
        LOG.debug("Finished: {}", result);
        if (tracker.existsInDatabase(result.toCoordinate(), lane)) {
            // reduce the opportunity for race-conditions by re-checking before storing
            return;
        }
        db.save(result);
        var m = msgs.getStd(result);
        m.consumedAt = consumedAt;
        kafka.publish(m, args.kafkaOut, lane);
    }

    private static void delayExecutionToPreventThrottling() {
        try {
            Thread.sleep(EXEC_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}