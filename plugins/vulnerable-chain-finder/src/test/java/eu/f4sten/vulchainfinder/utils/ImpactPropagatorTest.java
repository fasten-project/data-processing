/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.f4sten.vulchainfinder.utils;

import static eu.fasten.analyzer.javacgopal.data.CGAlgorithm.RTA;
import static eu.fasten.analyzer.javacgopal.data.CallPreservationStrategy.ONLY_STATIC_CALLSITES;
import static eu.fasten.core.utils.TestUtils.getTestResource;

import com.google.common.collect.HashBiMap;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.vulchainfinder.data.NodeImpacts;
import eu.fasten.analyzer.javacgopal.data.OPALCallGraphConstructor;
import eu.fasten.analyzer.javacgopal.data.OPALPartialCallGraphConstructor;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.MergedDirectedGraph;
import eu.fasten.core.data.PartialJavaCallGraph;
import eu.fasten.core.data.vulnerability.Vulnerability;
import eu.fasten.core.merge.CGMerger;
import eu.fasten.core.vulchains.VulnerableCallChain;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImpactPropagatorTest {

    @Test
    void testPropagateUrisImpacts() {

        //                 ┌─────────┐
        //                 │         │
        //       *    v2   *    *    ▼
        // 4◄────3───►2◄───5───►6───►7*
        //            │         │    │
        //            ▼         ▼    │
        //            8         1◄───┘
        //                      v1
        HashBiMap<Long, String> uris = HashBiMap.create(Map.of(1L, "v1", 2L, "v2"));
        final var vulUris = Set.of(FastenURI.create("v1"), FastenURI.create("v2"));
        final var dg = createDg();
        final var propagator = new ImpactPropagator(dg, uris);

        propagator.propagateUrisImpacts(vulUris);

        Assertions.assertEquals(getExpected(), propagator.getImpacts());
    }

    private Set<NodeImpacts> getExpected() {
        return Set.of(
            new NodeImpacts(2L,
                Map.of(
                    3L, 2L,
                    5L, 2L
                )
            ),
            new NodeImpacts(1L,
                Map.of(
                    5L, 6L,
                    6L, 1L,
                    7L, 1L
                )
            )
        );
    }

    private static MergedDirectedGraph createDg() {
        MergedDirectedGraph dg = new MergedDirectedGraph();
        addEdge(dg, 3, 4);
        addEdge(dg, 3, 2);
        addEdge(dg, 3, 3);
        addEdge(dg, 2, 8);
        addEdge(dg, 5, 2);
        addEdge(dg, 5, 6);
        addEdge(dg, 5, 7);
        addEdge(dg, 6, 1);
        addEdge(dg, 6, 7);
        addEdge(dg, 7, 1);
        return dg;
    }

    @Test
    void testARealWorldExample() {
        final var propagator = instantiatePropagatorFromResources();
        final var vulUri = vulUri();
        final var appUri = appUri();
        final var expected = expected();
        final var vulCallables = Map.of(vulUri, List.of(new Vulnerability()));

        propagator.propagateUrisImpacts(vulCallables.keySet());
        final var vulChains =
            propagator.extractApplicationVulChains(vulCallables, getMavenId());

        final var actual = FindFirstTargetNodeInVulChain(vulChains, appUri, vulUri);
        Assertions.assertEquals(expected, actual);
    }

    private MavenId getMavenId() {
        final var id = new MavenId();
        id.groupId = "com.alibaba.middleware";
        id.artifactId = "termd-core";
        id.version = "1.1.7.13-SNAPSHOT";
        return id;
    }

    private static ImpactPropagator instantiatePropagatorFromResources() {
        var deps = List.of(
            generateCG(getTestResource("termd-core-1.1.7.13-SNAPSHOT.jar"),
                "com.alibaba.middleware:termd-core", "1.1.7.13-SNAPSHOT"),
            generateCG(getTestResource("jackson-databind-2.7.9.4.jar"),
                "com.fasterxml.jackson.core:jackson-databind", "2.7.9.4"));

        final var merger = new CGMerger(deps);
        final var mergedCG = merger.mergeAllDeps();
        final var uris = merger.getAllUris();
        return new ImpactPropagator(mergedCG, uris);
    }

    private static FastenURI expected() {
        return FastenURI.create("fasten://mvn!com.fasterxml.jackson" +
            ".core:jackson-databind$2.7.9" +
            ".4/com.fasterxml.jackson.databind/ObjectMapper.readValue(%2Fjava" +
            ".lang%2FString,%2Fjava.lang%2FClass)%2Fjava.lang%2FObject");
    }

    private static FastenURI appUri() {
        return FastenURI.create("fasten://mvn!com.alibaba.middleware:termd-core$1.1.7" +
            ".13-SNAPSHOT/io.termd.core.http/HttpTtyConnection.writeToDecoder" +
            "(%2Fjava.lang%2FString)%2Fjava.lang%2FVoidType");
    }

    private static FastenURI vulUri() {
        return FastenURI.create(
            "fasten://mvn!com.fasterxml.jackson.core:jackson-databind$2.7.9.4/" +
                "com.fasterxml.jackson.databind.deser/BasicDeserializerFactory.createMapDeserializer" +
                "(%2Fcom.fasterxml.jackson.databind%2FDeserializationContext," +
                "%2Fcom.fasterxml.jackson.databind.type%2FMapType," +
                "%2Fcom.fasterxml.jackson.databind%2FBeanDescription)" +
                "%2Fcom.fasterxml.jackson.databind%2FJsonDeserializer");
    }

    private static void addEdge(MergedDirectedGraph dg, long source, long target) {
        dg.addVertex(source);
        dg.addVertex(target);
        dg.addEdge(source, target);
    }

    private FastenURI FindFirstTargetNodeInVulChain(final Set<VulnerableCallChain> vulChains,
                                                    final FastenURI appUri,
                                                    final FastenURI vulUri) {
        FastenURI result = null;
        for (final var vulChain : vulChains) {
            var chain = vulChain.getChain();
            if (chain.get(0).equals(appUri)) {
                if (chain.get(chain.size() - 1).equals(vulUri)) {
                    result = chain.get(1);
                }
            }
        }
        return result;
    }

    private static PartialJavaCallGraph generateCG(final File file, final String product,
                                                   final String version) {
        final var opalCG = new OPALCallGraphConstructor().construct(file, RTA);
        final var cg =
            new OPALPartialCallGraphConstructor().construct(opalCG, ONLY_STATIC_CALLSITES);

        return new PartialJavaCallGraph("mvn", product, version,
            -1, "opal", cg.classHierarchy, cg.graph);
    }

}