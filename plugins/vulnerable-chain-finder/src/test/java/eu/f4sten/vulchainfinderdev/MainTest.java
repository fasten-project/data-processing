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

//import static eu.f4sten.vulchainfinder.Main.extractMavenIdFrom;
//import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.f4sten.infra.impl.json.JsonUtilsImpl;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.vulchainfinderdev.json.FastenURIJacksonModule;
import eu.f4sten.vulchainfinderdev.utils.DatabaseUtils;
import eu.f4sten.vulchainfinderdev.utils.DependencyResolver;
import eu.fasten.core.data.callableindex.RocksDao;
//import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.vulchains.VulnerableCallChain;
import eu.fasten.core.vulchains.VulnerableCallChainJsonUtils;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.rocksdb.RocksDBException;

/**
 * This is an integration test for the vuln-chain-finder plugin. It requires a FASTEN's Docker Compose setup or
 * access to the production server. The test is based on an existing Maven package and a known vulnerability in it.
 */
class MainTest {

    public static final String BASE_DIR = System.getenv("BASE_DIR");
    public static final String CI_URL = String.valueOf(Paths.get(BASE_DIR, "callable-index"));
    private static final String DB_URL = System.getenv("DB_ADDR");
    private static final String USR = System.getenv("DB_USER");
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    public static final String LOCAL_REST = "http://localhost:9080/";
    public static final String LOCAL_DEP_RESOLVER = System.getenv("DGR_ADDR");
    public static final Path VUL_REPO =
        Paths.get("src", "test", "resources", "vulnrepo");

//    @Test
//    void testExtractMavenIdFrom() {
//        final var pomAR = new Pom("g", "g", "?", "v");
//
//        var actual = extractMavenIdFrom(pomAR);
//        final MavenId expected = new MavenId();
//        expected.groupId = pomAR.groupId;
//        expected.artifactId = pomAR.artifactId;
//        expected.version = pomAR.version;
//        assertEquals(expected, actual);
//    }

    @Disabled("This is an integration test that checks if all the steps of vul-chain-finder work " +
        "correctly.")
    @Test
    void testProcess() throws RocksDBException, IOException {
        final var id = getMavenId("org.apache.wicket", "wicket-request", "6.13.0");
        final Main main = setUpMainFor(id);

        main.process();

        final var actual = readResourceIntoVulCC(readResourceIntoString(createResourceNameFromID(id)));
        final var expected = readResourceIntoVulCC(readResourceIntoString(createResourceNameFromID(id,"_expected")));

        testTwoUnorderedVulChainRepos(actual, expected);
    }
    @Test
    void testProcessWitnNoVCC() throws RocksDBException, IOException {
        final var id = getMavenId("net.spy", "spymemcached", "2.12.3");
        final Main main = setUpMainFor(id);

        main.process();
        final var emptyVulCC = readResourceIntoVulCC(readResourceIntoString(createResourceNameFromID(id)));
        Assertions.assertEquals(emptyVulCC.size(), 0);
    }

    private void testTwoUnorderedVulChainRepos(final Set<VulnerableCallChain> actual, final Set<VulnerableCallChain> expected) {
        Assertions.assertEquals(actual.size(), expected.size());

        var vccMatch = 0;
        for (var vce : expected) {
            for (var vca: actual) {
                if (vce.getVulnerabilities().equals(vca.getVulnerabilities()) &&
                        vce.getChain().equals(vca.getChain())){
                    vccMatch++;
                    break;
                }
            }
        }
        Assertions.assertEquals(vccMatch, expected.size());
    }

    private String createResourceNameFromID(final MavenId id) {
        return id.groupId + "-" + id.artifactId + "-" + id.version + ".json";
    }

    private String createResourceNameFromID(final MavenId id, final String suffix) {
        return id.groupId + "-" + id.artifactId + "-" + id.version + suffix + ".json";
    }

    private Main setUpMainFor(final MavenId id) throws RocksDBException, FileNotFoundException {
        final var dbContext = DSL.using(DB_URL, USR, System.getenv("PG_PWD"));
        final var om = new ObjectMapper().registerModule(new FastenURIJacksonModule());
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final var jsonUtils = new JsonUtilsImpl(om);
        final var db = new DatabaseUtils(dbContext, jsonUtils);
        final var ci = new RocksDao(CI_URL, true);
        final var resolver = new DependencyResolver(LOCAL_REST, LOCAL_DEP_RESOLVER, HTTP_CLIENT);
        final var repo = new VulnerableCallChainRepository(VUL_REPO.toString());
        final var ioUtils = mock(IoUtils.class);
        when(ioUtils.getBaseFolder()).thenReturn(new File(BASE_DIR));
        final var main = new Main(db, ci, mock(Kafka.class), new VulChainFinderArgs(),
            mock(MessageGenerator.class), resolver, repo, ioUtils);
        main.setCurId(id);
        return main;
    }

    private String readResourceIntoString(final String name) throws IOException {

        final var path = Paths.get(VUL_REPO.toAbsolutePath().toString(), name);
        return Files.readString(path);
    }

    private Set<VulnerableCallChain> readResourceIntoVulCC(final String jsonStr) {
        final var vulCCType = new TRef<Set<VulnerableCallChain>>() {};
        return VulnerableCallChainJsonUtils.fromJson(jsonStr, vulCCType.getType());
    }


    private MavenId getMavenId(final String g, final String a, final String v) {
        final var id = new MavenId();
        id.groupId = g;
        id.artifactId = a;
        id.version = v;
        id.artifactRepository = null;
        id.packagingType = "jar";
        return id;
    }
}