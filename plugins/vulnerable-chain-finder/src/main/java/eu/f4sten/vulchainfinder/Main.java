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

package eu.f4sten.vulchainfinder;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.vulchainfinder.utils.DatabaseUtils;
import eu.f4sten.vulchainfinder.utils.ImpactPropagator;
import eu.f4sten.vulchainfinder.utils.RestAPIDependencyResolver;
import eu.fasten.core.data.callableindex.RocksDao;
import eu.fasten.core.merge.CGMerger;
import eu.fasten.core.vulchains.VulnerableCallChain;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public final RestAPIDependencyResolver resolver;
    private final DatabaseUtils db;
    private final RocksDao dao;
    private final Kafka kafka;
    private final VulChainFinderArgs args;
    private final MessageGenerator msgs;
    private final VulnerableCallChainRepository repo;

    private MavenId curId;

    @Inject
    public Main(DatabaseUtils db, RocksDao dao, Kafka kafka, VulChainFinderArgs args,
                MessageGenerator msgs, RestAPIDependencyResolver resolver,
                VulnerableCallChainRepository repo) {
        this.db = db;
        this.dao = dao;
        this.kafka = kafka;
        this.args = args;
        this.msgs = msgs;
        this.resolver = resolver;
        this.repo = repo;
    }

    @Override
    public void run() {
        AssertArgs.assertFor(args)
            .notNull(a -> a.kafkaIn, "kafka input topic")
            .notNull(a -> a.kafkaOut, "kafka output topic");

        LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);

        final var msgClass = new TRef<Message<Message<Message<Message
            <MavenId, PomAnalysisResult>, Object>, Object>, Object>>() {
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
        LOG.info("Processing {}", curId.asCoordinate());

        final var allDeps = resolver.resolveDependencyIds(curId);

        final var vulDeps = db.selectVulnerablePackagesExistingIn(allDeps);

        Set<VulnerableCallChain> vulChains = new HashSet<>();
        if (curIdIsPackageLevelVulnerable(vulDeps)) {
            vulChains = extractVulCallChains(allDeps, vulDeps);
        }

        if (curIdIsMethodLevelVulnerable(vulChains)) {
            storeInVulRepo(vulChains);
        }
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

    private Set<VulnerableCallChain> extractVulCallChains(final Set<Long> allDeps,
                                                          final Set<Long> vulDeps) {
        Set<VulnerableCallChain> result = new HashSet<>();

        final var merger = new CGMerger(allDeps, db.getContext(), dao);
        final var mergedCG = merger.mergeAllDeps();
        final var vulCallables = db.selectVulCallablesOf(vulDeps);
        final var propagator = new ImpactPropagator(mergedCG, merger.getAllUrisFromDB(mergedCG));
        propagator.propagateUrisImpacts(vulCallables.keySet());
        LOG.info("Found {} distinct vulnerable paths", propagator.getImpacts().size());

        if (!propagator.getImpacts().isEmpty()) {
            result = propagator.extractApplicationVulChains(vulCallables, curId);
        }

        return result;
    }

    public static MavenId extractMavenIdFrom(final PomAnalysisResult pomAnalysisResult) {
        final var mavenId = new MavenId();
        mavenId.groupId = pomAnalysisResult.groupId;
        mavenId.artifactId = pomAnalysisResult.artifactId;
        mavenId.version = pomAnalysisResult.version;
        return mavenId;
    }

    private void runOrPublishErr(final Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            LOG.warn("Execution failed for input: {}", curId, e);

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

}