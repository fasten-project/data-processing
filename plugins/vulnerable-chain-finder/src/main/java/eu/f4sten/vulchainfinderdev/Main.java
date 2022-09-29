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

package eu.f4sten.vulchainfinderdev;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.vulchainfinderdev.data.VcfPayload;
import eu.f4sten.vulchainfinderdev.exceptions.AnalysisTimeOutException;
import eu.f4sten.vulchainfinderdev.exceptions.RestApiError;
import eu.f4sten.vulchainfinderdev.exceptions.VulnChainRepoSizeLimitException;
import eu.f4sten.vulchainfinderdev.utils.DatabaseUtils;
import eu.f4sten.vulchainfinderdev.utils.ImpactPropagator;
import eu.f4sten.vulchainfinderdev.utils.DependencyResolver;
import eu.fasten.analyzer.javacgopal.data.CGAlgorithm;
import eu.fasten.analyzer.javacgopal.data.OPALCallGraph;
import eu.fasten.analyzer.javacgopal.data.OPALCallGraphConstructor;
import eu.fasten.analyzer.javacgopal.data.OPALPartialCallGraphConstructor;
import eu.fasten.core.data.CallPreservationStrategy;
import eu.fasten.core.data.Constants;
import eu.fasten.core.data.DirectedGraph;
import eu.fasten.core.data.MergedDirectedGraph;
import eu.fasten.core.data.PartialJavaCallGraph;
import eu.fasten.core.data.callableindex.RocksDao;

import eu.fasten.core.data.opal.MavenArtifactDownloader;
import eu.fasten.core.data.opal.MavenCoordinate;
//import eu.fasten.core.exceptions.UnrecoverableError;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.utils.TypeToJarMapper;
import eu.fasten.core.vulchains.VulnerableCallChain;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public final DependencyResolver resolver;
    private final DatabaseUtils db;
    private final RocksDao dao;
    private final Kafka kafka;
    private final VulChainFinderArgs args;
    private final MessageGenerator msgs;
    private final VulnerableCallChainRepository repo;
    private final String baseDir;
    private final Path m2Path;
    private Lane kafkaLane;

    private MavenId curId;
    // Max. number of vuln chain repos can be stored on the disk to avoid the OoM error
    final private int vulnChainsRepoSizeLimit = 50000;
    final private int analysisTimeOut = 25; // minutes

    static class LocalDirectedGraph {
        DirectedGraph graph;
        BiMap<Long, String> graphUris;
    }

    @Inject
    public Main(DatabaseUtils db, RocksDao dao, Kafka kafka, VulChainFinderArgs args,
                MessageGenerator msgs, DependencyResolver resolver,
                VulnerableCallChainRepository repo, IoUtils io) {
        this.db = db;
        this.dao = dao;
        this.kafka = kafka;
        this.args = args;
        this.msgs = msgs;
        this.resolver = resolver;
        this.repo = repo;
        this.baseDir = io.getBaseFolder().getAbsolutePath();
        this.m2Path = Paths.get(this.baseDir, ".m2", "repository");

        if(args.publishToKafka) {
            LOG.info("The plugin publishes pkg. versions with vuln. call chains to its output Kafka topic.");
        }
    }

    @Override
    public void run() {
        AssertArgs.assertFor(args)
            .notNull(a -> a.kafkaIn, "kafka input topic")
            .notNull(a -> a.kafkaOut, "kafka output topic");

        LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);

        final var msgClass = new TRef<Message<Message<Message<Message
                        <MavenId, Pom>, Object>, Object>, Object>>() {
        };

        kafka.subscribe(args.kafkaIn, msgClass, (msg, l) -> {
            final var pomAnalysisResult = msg.input.input.input.payload;
            curId = extractMavenIdFrom(pomAnalysisResult);
            kafkaLane = l;
            LOG.info("Consuming next record ...");
            runOrPublishErr(this::process);
        });
        while (true) {
            LOG.debug("Polling ...");
            kafka.poll();
        }
    }

    public void process() {
        // NOTE: this can be a temporary FS-based check and can be replaced with a better approach or removed at all.
        if (isCurIdProcessed()) {
            LOG.info("Coordinate {} already processed!", curId.asCoordinate());
            return;
        }

        LOG.info("Processing {}", curId.asCoordinate());

        // Client/Root package
        var clientPkgVer = new Pair<Long, Pair<MavenId, File>>(db.getPackageVersionID(curId),
                new Pair<>(curId, new File(Paths.get(String.valueOf(m2Path), curId.toJarPath()).toString())));
        final var clientPkgVerAllDeps = resolver.resolveDependencies(curId, this.m2Path, this.db);

        // Download jars if not present in the .m2 folder
        if (!clientPkgVer.getSecond().getSecond().exists()) {
            clientPkgVer.getSecond().getSecond().getParentFile().mkdirs();
            new MavenArtifactDownloader(MavenCoordinate.fromString(clientPkgVer.getSecond().getFirst().asCoordinate(),
                    clientPkgVer.getSecond().getFirst().packagingType),
                    clientPkgVer.getSecond().getSecond()).downloadArtifact(null);
        }

        var resolvedClientPkgVerDeps = new HashSet<Pair<Long, Pair<MavenId, File>>>();
        for (var d : clientPkgVerAllDeps) {
            // Ignore client/root package in the dep. set
            if (d.getFirst().equals(clientPkgVer.getFirst())) {
                continue;
            }

            if (!d.getSecond().getSecond().exists()) {
                d.getSecond().getSecond().getParentFile().mkdirs();
                try {
                    var f = new MavenArtifactDownloader(MavenCoordinate.fromString(d.getSecond().getFirst().asCoordinate(),
                            d.getSecond().getFirst().packagingType),
                            d.getSecond().getSecond()).downloadArtifact(null);
                    if (f != null) {
                        resolvedClientPkgVerDeps.add(d);
                    }
                } catch (MissingArtifactException e) {
                    LOG.error("Could not retrieve artifact for {}", d.getSecond().getFirst().asCoordinate());
                }
            }
            else {
                resolvedClientPkgVerDeps.add(d);
            }
        }

        LOG.info("Resolved {} dependencies for {}", resolvedClientPkgVerDeps.size(), curId.asCoordinate());
        // Client's (transitive) dependency set
        final var allDeps = new LinkedHashSet<Long>();
        resolvedClientPkgVerDeps.forEach(d -> allDeps.add(d.getFirst()));
        // DO NOT consider the client/root package when looking for vulnerabilities in the dependency tree
        // allDeps.add(clientPkgVer.getFirst());

        final var vulDeps = db.selectVulnerablePackagesExistingIn(allDeps);
        LOG.info("Found {} known vulnerabilities in the dep. set of {}", vulDeps.size(), curId.asCoordinate());

        Set<VulnerableCallChain> vulChains = new HashSet<>();
        if (curIdIsPackageLevelVulnerable(vulDeps)) {
            vulChains = extractVulCallChains(clientPkgVer, resolvedClientPkgVerDeps, vulDeps);
        }

        //curIdIsMethodLevelVulnerable(vulChains);
        // NOTE: it stores empty vuln. chains too to avoid re-processing records.
        if (vulChains.size() <= this.vulnChainsRepoSizeLimit) {
            final var vulRepoFilePath = storeInVulRepo(vulChains);
            if (args.publishToKafka) {
                kafka.publish(new VcfPayload(curId.groupId, curId.artifactId, curId.version, curId.packagingType,
                                vulRepoFilePath, vulChains.size()), args.kafkaOut, kafkaLane);
            }
        } else {
            throw new VulnChainRepoSizeLimitException("Cannot store a vuln chain repo with the size of " + vulChains.size());
        }
    }

    private boolean curIdIsMethodLevelVulnerable(final Set<VulnerableCallChain> vulChains) {
        return !vulChains.isEmpty();
    }

    private boolean curIdIsPackageLevelVulnerable(final Set<Long> vulDeps) {
        return vulDeps != null && !vulDeps.isEmpty();
    }

    private String storeInVulRepo(final Set<VulnerableCallChain> vulnerableCallChains) {
        final var productName = String.format("%s:%s", curId.groupId, curId.artifactId);
        return repo.store(productName, curId.version, vulnerableCallChains);
    }

    private Set<VulnerableCallChain> extractVulCallChains(final Pair<Long, Pair<MavenId, File>> clientPkgVer,
                                                          final Set<Pair<Long, Pair<MavenId, File>>> allDeps,
                                                          final Set<Long> vulDeps) {
        AtomicReference<Set<VulnerableCallChain>> result = new AtomicReference<>();
        result.set(new HashSet<>());
        final var vulCallables = db.selectVulnerableCallables(vulDeps);
        LOG.info("Found {} vulnerable callables in the dep. set of {}", vulCallables.size(), clientPkgVer.getSecond().getFirst().asCoordinate());

        Future future = null;
        if (!vulCallables.isEmpty()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            future = executor.submit(() -> {
                // Merging using OPAL
                OPALCallGraph opalCallGraph = new OPALCallGraphConstructor().construct(new File[]{clientPkgVer.getSecond().getSecond()},
                        extractFilesFromDeps(allDeps), CGAlgorithm.CHA);

                var opalPartialCallGraph = new OPALPartialCallGraphConstructor().construct(opalCallGraph,
                        CallPreservationStrategy.INCLUDING_ALL_SUBTYPES);

                var partialCallGraph = new PartialJavaCallGraph(Constants.mvnForge, clientPkgVer.getSecond().getFirst().getProductName(),
                        clientPkgVer.getSecond().getFirst().getProductVersion(), -1,
                        Constants.opalGenerator, opalPartialCallGraph.classHierarchy, opalPartialCallGraph.graph);
                LOG.info("Created a partial call graph w/ {} call sites for {} and its dependencies", partialCallGraph.getCallSites().size(),
                        clientPkgVer.getSecond().getFirst().asCoordinate());

                var localMergedCG = PCGtoLocalDirectedGraph(partialCallGraph,
                        createTypeUriToCoordMap(clientPkgVer, allDeps));

                final var propagator = new ImpactPropagator(localMergedCG.graph, localMergedCG.graphUris);
                propagator.propagateUrisImpacts(vulCallables.keySet());
                LOG.info("Found {} distinct vulnerable paths", propagator.getImpacts().size());

                if (!propagator.getImpacts().isEmpty()) {
                    result.get().addAll(propagator.extractApplicationVulChains(vulCallables, curId));
                    LOG.info("Found {} vulnerable call chains from client to its dependencies", result.get().size());
                }
            });
        }
        try {
            if (future != null) {
                future.get(this.analysisTimeOut, TimeUnit.MINUTES);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new AnalysisTimeOutException("Could not analyze " + clientPkgVer.getSecond().getFirst().asCoordinate() +
                    " in " + this.analysisTimeOut + " minutes.");
        }
        return result.get();
    }

    /**
     * A utility method to fix type URIs for the whole-program CG generated by the OPAL API
     */
    private Map<String, String> createTypeUriToCoordMap(final Pair<Long, Pair<MavenId, File>> clientPkgVer,
                                                        final Set<Pair<Long, Pair<MavenId, File>>> allDeps) {
        final var jars = new ArrayList<org.apache.commons.lang3.tuple.Pair<MavenCoordinate, File>>();
        for (final var d: allDeps) {
            final var mavenCoord = d.getSecond().getFirst().toMavenCoordinate();
            final var jarFile = d.getSecond().getSecond();
            jars.add(org.apache.commons.lang3.tuple.Pair.of(mavenCoord, jarFile));
        }
        jars.add(org.apache.commons.lang3.tuple.Pair.of(clientPkgVer.getSecond().getFirst().toMavenCoordinate(),
                clientPkgVer.getSecond().getSecond()));
        return TypeToJarMapper.createTypeUriToCoordMap(jars);
    }

    public static MavenId extractMavenIdFrom(final Pom pomAnalysisResult) {
        final var mavenId = new MavenId();
        mavenId.groupId = pomAnalysisResult.groupId;
        mavenId.artifactId = pomAnalysisResult.artifactId;
        mavenId.version = pomAnalysisResult.version;
        mavenId.packagingType = pomAnalysisResult.packagingType;
        return mavenId;
    }

    public LocalDirectedGraph PCGtoLocalDirectedGraph(@NotNull final PartialJavaCallGraph rcg,
                                                      final Map<String, String> typeUriToCoords) {
        var localDirectedGraph = new LocalDirectedGraph();
        localDirectedGraph.graph = new MergedDirectedGraph();
        localDirectedGraph.graphUris = HashBiMap.create();

        final var internals = rcg.mapOfFullURIStrings(typeUriToCoords);
        for (final var intInt : rcg.getGraph().getCallSites().entrySet()) {
            if (internals.containsKey(intInt.getKey().firstInt())
                    && internals.containsKey(intInt.getKey().secondInt())) {
                final var source = (long) intInt.getKey().firstInt();
                final var target = (long) intInt.getKey().secondInt();
                localDirectedGraph.graph.addVertex(source);
                localDirectedGraph.graph.addVertex(target);
                localDirectedGraph.graph.addEdge(source, target);

                // Create the FURI Map
                final var sourceUri = internals.get(intInt.getKey().firstInt());
                final var targetUri = internals.get(intInt.getKey().secondInt());
                if (!localDirectedGraph.graphUris.containsKey(source)) {
                    localDirectedGraph.graphUris.put(source, sourceUri);
                }
                if (!localDirectedGraph.graphUris.containsKey(target)) {
                    localDirectedGraph.graphUris.put(target, targetUri);
                }
            }
        }
        return localDirectedGraph;
    }

    private File[] extractFilesFromDeps(final Set<Pair<Long, Pair<MavenId, File>>> deps) {
        var allDepsFile = new File[deps.size()];
        var i = 0;
        for (var f: deps) {
            allDepsFile[i] = f.getSecond().getSecond();
            i++;
        }
        return allDepsFile;
    }

    private void runOrPublishErr(final Runnable r) {
        try {
            r.run();
        } catch (RestApiError e) {
            LOG.error("Forced to stop the plug-in as the REST API is unavailable", e);
            throw e;
        } catch (Exception e) {
            LOG.error("Execution failed for input: {} and", curId, e);
            //throw new UnrecoverableError("the plugin should be stopped.");

            var msg = msgs.getErr(curId, returnCause(e));
            kafka.publish(msg, args.kafkaOut, Lane.ERROR);
        }
    }

    private Throwable returnCause(final Exception e) {
        final var isRunTime = RuntimeException.class.equals(e.getClass());
        final var causeNotNull = e.getCause() != null;
        if (isRunTime && causeNotNull) {
            return e.getCause();
        }
        return e;
    }

    public MavenId getCurId() {
        return curId;
    }

    public void setCurId(final MavenId curId) {
        this.curId = curId;
    }

    private boolean isCurIdProcessed() {
        return new File(repo.getFilePath(String.format("%s:%s", curId.groupId, curId.artifactId), curId.version)).exists();
    }

}