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
import eu.f4sten.vulchainfinderdev.exceptions.RestApiError;
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
import eu.fasten.core.data.metadatadb.codegen.tables.Callables;
import eu.fasten.core.data.metadatadb.codegen.tables.Modules;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import eu.fasten.core.data.opal.MavenArtifactDownloader;
import eu.fasten.core.data.opal.MavenCoordinate;
//import eu.fasten.core.exceptions.UnrecoverableError;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.vulchains.VulnerableCallChain;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
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

    private MavenId curId;

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

        LOG.info("Resolved {} dependencies for {}", clientPkgVerAllDeps.size(), curId.asCoordinate());
        // Client's (transitive) dependency set + client itself
        final var allDeps = new HashSet<Long>();
        resolvedClientPkgVerDeps.forEach(d -> allDeps.add(d.getFirst()));
        allDeps.add(clientPkgVer.getFirst());

        final var vulDeps = db.selectVulnerablePackagesExistingIn(allDeps);
        LOG.info("Found {} known vulnerabilities in the dep. set of {}", vulDeps.size(), curId.asCoordinate());

        Set<VulnerableCallChain> vulChains = new HashSet<>();
        if (curIdIsPackageLevelVulnerable(vulDeps)) {
            vulChains = extractVulCallChains(clientPkgVer, resolvedClientPkgVerDeps, vulDeps);
        }

        curIdIsMethodLevelVulnerable(vulChains);
        // NOTE: it stores empty vuln. chains too to avoid re-processing records.
        storeInVulRepo(vulChains);
    }

    private boolean curIdIsMethodLevelVulnerable(final Set<VulnerableCallChain> vulChains) {
        return !vulChains.isEmpty();
    }

    private boolean curIdIsPackageLevelVulnerable(final Set<Long> vulDeps) {
        return vulDeps != null && !vulDeps.isEmpty();
    }

    private void storeInVulRepo(final Set<VulnerableCallChain> vulnerableCallChains) {
        final var productName = String.format("%s:%s", curId.groupId, curId.artifactId);
        repo.store(productName, curId.version, vulnerableCallChains);
    }

    private Set<VulnerableCallChain> extractVulCallChains(final Pair<Long, Pair<MavenId, File>> clientPkgVer,
                                                          final Set<Pair<Long, Pair<MavenId, File>>> allDeps,
                                                          final Set<Long> vulDeps) {
        Set<VulnerableCallChain> result = new HashSet<>();

        // Merging using OPAL
        OPALCallGraph opalCallGraph = new OPALCallGraphConstructor().construct(new File[] {clientPkgVer.getSecond().getSecond()},
                extractFilesFromDeps(allDeps), CGAlgorithm.CHA);
        LOG.info("Generated an OPAL call graph for {} and its dependencies", clientPkgVer.getSecond().getFirst().asCoordinate());

        var opalPartialCallGraph = new OPALPartialCallGraphConstructor().construct(opalCallGraph, CallPreservationStrategy.ONLY_STATIC_CALLSITES);

        var partialCallGraph = new PartialJavaCallGraph(Constants.mvnForge, clientPkgVer.getSecond().getFirst().getProductName(),
                clientPkgVer.getSecond().getFirst().getProductVersion(), -1,
                Constants.opalGenerator, opalPartialCallGraph.classHierarchy, opalPartialCallGraph.graph);
        var mergedCG = PCGtoLocalDirectedGraph(partialCallGraph);
        LOG.info("Created a partial call graph for {} and its dependencies", clientPkgVer.getSecond().getFirst().asCoordinate());

        final var vulCallables = db.selectVulCallablesOf(vulDeps);
        LOG.info("Found {} vulnerable callables in the dep. set of {}", vulCallables.size(), clientPkgVer.getSecond().getFirst().asCoordinate());
        final var propagator = new ImpactPropagator(mergedCG, getAllUrisFromDB(mergedCG));
        propagator.propagateUrisImpacts(vulCallables.keySet());
        LOG.info("Found {} distinct vulnerable paths", propagator.getImpacts().size());

        if (!propagator.getImpacts().isEmpty()) {
            result = propagator.extractApplicationVulChains(vulCallables, curId);
            LOG.info("Found {} vulnerable call chains from client to its dependencies", result.size());
        }

        return result;
    }

    public static MavenId extractMavenIdFrom(final Pom pomAnalysisResult) {
        final var mavenId = new MavenId();
        mavenId.groupId = pomAnalysisResult.groupId;
        mavenId.artifactId = pomAnalysisResult.artifactId;
        mavenId.version = pomAnalysisResult.version;
        mavenId.packagingType = pomAnalysisResult.packagingType;
        return mavenId;
    }

    public DirectedGraph PCGtoLocalDirectedGraph(@NotNull final PartialJavaCallGraph rcg) {
        DirectedGraph dcg = new MergedDirectedGraph();
        final var internals = rcg.mapOfFullURIStrings();
        for (final var intInt : rcg.getGraph().getCallSites().entrySet()) {
            if (internals.containsKey(intInt.getKey().firstInt())
                    && internals.containsKey(intInt.getKey().secondInt())) {
                final var source = (long) intInt.getKey().firstInt();
                final var target = (long) intInt.getKey().secondInt();
                dcg.addVertex(source);
                dcg.addVertex(target);
                dcg.addEdge(source, target);
            }
        }
        return dcg;
    }

    /**
     * Gets all the FASTEN URIs of a given directed graph from the metadata DB
     * @param dg
     * @return
     */
    public BiMap<Long, String> getAllUrisFromDB(DirectedGraph dg){
        Set<Long> gIDs = new HashSet<>();
        for (Long node : dg.nodes()) {
            if (node > 0) {
                gIDs.add(node);
            }
        }
        BiMap<Long, String> uris = HashBiMap.create();
        db.getContext()
                .select(Callables.CALLABLES.ID, Packages.PACKAGES.PACKAGE_NAME,
                        PackageVersions.PACKAGE_VERSIONS.VERSION,
                        Callables.CALLABLES.FASTEN_URI)
                .from(Callables.CALLABLES, Modules.MODULES, PackageVersions.PACKAGE_VERSIONS, Packages.PACKAGES)
                .where(Callables.CALLABLES.ID.in(gIDs))
                .and(Modules.MODULES.ID.eq(Callables.CALLABLES.MODULE_ID))
                .and(PackageVersions.PACKAGE_VERSIONS.ID.eq(Modules.MODULES.PACKAGE_VERSION_ID))
                .and(Packages.PACKAGES.ID.eq(PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID))
                .fetch().forEach(record -> uris.put( record.component1(),
                "fasten://mvn!" + record.component2() + "$" + record.component3() + record.component4()));

        return uris;
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