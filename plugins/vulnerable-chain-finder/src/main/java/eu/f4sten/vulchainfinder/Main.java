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
import eu.f4sten.vulchainfinder.utils.CallableIndexUtils;
import eu.f4sten.vulchainfinder.utils.DatabaseUtils;
import eu.f4sten.vulchainfinder.utils.ImpactPropagator;
import eu.f4sten.vulchainfinder.utils.RestAPIDependencyResolver;
import eu.fasten.core.merge.CGMerger;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;
import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private final DatabaseUtils db;
    private final CallableIndexUtils ci;
    private final Kafka kafka;
    private final VulChainFinderArgs args;
    private final MessageGenerator msgs;

    private MavenId curId;

    @Inject
    public Main(DatabaseUtils db, CallableIndexUtils ci, Kafka kafka, VulChainFinderArgs args,
                MessageGenerator msgs) {
        this.db = db;
        this.ci = ci;
        this.kafka = kafka;
        this.args = args;
        this.msgs = msgs;
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
        final var resolver = new RestAPIDependencyResolver(args.restApiBaseURL, HTTP_CLIENT);
        final var depIds = resolver.resolveDependencyIds(curId);
        final var vulDeps = db.selectVulnerablePackagesExistingIn(depIds);
        if (vulDeps == null ||  vulDeps.isEmpty()) {
            return;
        }
        final var merger = new CGMerger(depIds, db.getContext(), ci.getDao());
        final var mergedCG = merger.mergeAllDeps();
        final var idUriMap = merger.getAllUrisFromDB(mergedCG);
        final var vulCallables = db.selectVulCallablesOf(vulDeps);
        final var propagator = new ImpactPropagator(mergedCG, idUriMap);
        propagator.propagateUrisImpacts(vulCallables.keySet());
        LOG.info("Found {} distinct vulnerable paths", propagator.getImpacts().size());

        if (propagator.getImpacts().isEmpty()) {
            return;
        }

        final var vulnerableCallChains =
            propagator.extractApplicationVulChains(vulCallables, curId);
        if (vulnerableCallChains.isEmpty()) {
            return;
        }

        final var vulRepo = initializeVulRepo();
        final var productName = String.format("%s:%s", curId.groupId, curId.artifactId);
        vulRepo.store(productName, curId.version, vulnerableCallChains);
    }

    private VulnerableCallChainRepository initializeVulRepo() {
        try {
            return new VulnerableCallChainRepository(args.vulnChainRepoUrl);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    public void setCurId(MavenId curId) {
        this.curId = curId;
    }

}