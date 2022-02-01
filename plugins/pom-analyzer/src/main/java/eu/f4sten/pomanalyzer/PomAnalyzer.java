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
import eu.fasten.core.utils.Asserts;

public class PomAnalyzer implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(PomAnalyzer.class);

    private final MavenRepositoryUtils repo;
    private final EffectiveModelBuilder modelBuilder;
    private final PomExtractor extractor;
    private final DatabaseUtils db;
    private final Resolver resolver;
    private final Kafka kafka;
    private final MyArgs args;
    private final MessageGenerator msgs;
    private final PackagingFixer fixer;

    @Inject
    public PomAnalyzer(MavenRepositoryUtils repo, EffectiveModelBuilder modelBuilder, PomExtractor extractor,
            DatabaseUtils db, Resolver resolver, Kafka kafka, MyArgs args, MessageGenerator msgs,
            PackagingFixer fixer) {
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
        kafka.subscribe(args.kafkaIn, MavenId.class, (id, l) -> {
            LOG.info("Consuming next record ...");
            LOG.debug("{}", id);
            runAndCatch(id, () -> {
                var artifact = bootstrapFirstResolutionResultFromInput(id);
                if (!artifact.localPomFile.exists()) {
                    artifact.localPomFile = repo.downloadPomToTemp(artifact);
                }
                process(artifact, l, id, new HashSet<>());
            });
        });
        while (true) {
            LOG.debug("Polling ...");
            kafka.poll();
        }
    }

    private void runAndCatch(MavenId id, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            LOG.warn("Execution failed for input: {}", id, t);

            boolean isRuntimeExceptionAndNoSubtype = RuntimeException.class.equals(t.getClass());
            boolean isWrapped = isRuntimeExceptionAndNoSubtype && t.getCause() != null;

            var msg = msgs.getErr(id, isWrapped ? t.getCause() : t);
            kafka.publish(msg, args.kafkaOut, Lane.ERROR);
        }
    }

    private static ResolutionResult bootstrapFirstResolutionResultFromInput(MavenId id) {
        Asserts.assertNotNull(id.groupId);
        Asserts.assertNotNull(id.artifactId);
        Asserts.assertNotNull(id.version);

        var groupId = id.groupId.strip();
        var artifactId = id.artifactId.strip();
        var version = id.version.strip();
        var coord = asMavenCoordinate(groupId, artifactId, version);

        String artifactRepository = MavenUtilities.MAVEN_CENTRAL_REPO;
        if (id.artifactRepository != null) {
            var val = id.artifactRepository.strip();
            if (!val.isEmpty()) {
                artifactRepository = val;
            }
        }
        return new ResolutionResult(coord, artifactRepository);
    }

    private static String asMavenCoordinate(String groupId, String artifactId, String version) {
        // packing type is unknown
        return String.format("%s:%s:?:%s", groupId, artifactId, version);
    }

    private void process(ResolutionResult artifact, Lane lane, MavenId originalInput, Set<String> finished) {
        if (hasBeenIngested(artifact.coordinate, lane)) {
            LOG.info("Coordinate {} has already been ingested. Skipping.", artifact.coordinate);
            return;
        }
        LOG.info("Processing {} ...", artifact.coordinate);

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

        finished.add(artifact.coordinate);
        finished.add(result.toCoordinate());

        // resolve dependencies to
        // 1) have dependencies
        // 2) identify artifact sources
        // 3) make sure all dependencies exist in local .m2 folder
        var deps = resolver.resolveDependenciesFromPom(artifact.localPomFile);

        // resolution can be different for dependencies, so 'process' them independently
        deps.forEach(dep -> {
            if (finished.contains(dep.coordinate)) {
                LOG.warn("Detected cyclic dependency, skipping coordinate {}", dep.coordinate);
                return;
            }

            runAndCatch(originalInput, () -> {
                process(dep, lane, originalInput, new HashSet<>(finished));
            });
        });

        // to stay crash resilient, only mark once all dependencies have been processed
        db.markAsIngestedPackage(artifact.coordinate, lane);
        db.markAsIngestedPackage(result.toCoordinate(), lane);
    }

    private void store(PomAnalysisResult result, Lane lane, Date consumedAt) {
        LOG.debug("Finished: {}", result);
        if (hasBeenIngested(result.toCoordinate(), lane)) {
            // reduce the opportunity for race-conditions by re-checking before storing
            return;
        }
        db.save(result);
        var m = msgs.getStd(result);
        m.consumedAt = consumedAt;
        kafka.publish(m, args.kafkaOut, lane);
    }

    private boolean hasBeenIngested(String coordinate, Lane lane) {
        return lane == Lane.NORMAL
                ? db.hasPackageBeenIngested(coordinate, NORMAL) || db.hasPackageBeenIngested(coordinate, PRIORITY)
                : db.hasPackageBeenIngested(coordinate, lane);
    }
}