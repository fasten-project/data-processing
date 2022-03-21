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

import static eu.f4sten.vulchainfinder.Main.extractMavenIdFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.f4sten.infra.impl.json.JsonUtilsImpl;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.vulchainfinder.json.FastenURIJacksonModule;
import eu.f4sten.vulchainfinder.utils.DatabaseUtils;
import eu.f4sten.vulchainfinder.utils.RestAPIDependencyResolver;
import eu.fasten.core.data.callableindex.RocksDao;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

class MainTest {

    public static final String CI_URL =
        "/Users/mehdi/Desktop/MyMac/TUD/FASTEN/Repositories/MainRepo/fasten-docker-deployment/docker-volumes/fasten/java/callable-index";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/fasten_java";
    private static final String USR = "fasten";

    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    public static final String LOCAL_REST = "http://localhost:9080/";
    public static final Path VUL_REPO =
        Paths.get("src", "test", "resources", "vulnrepo");

    @Test
    void testExtractMavenIdFrom() {
        final var pomAR = new PomAnalysisResult();
        pomAR.groupId = "g";
        pomAR.artifactId = "g";
        pomAR.version = "v";

        var actual = extractMavenIdFrom(pomAR);
        final MavenId expected = new MavenId();
        expected.groupId = pomAR.groupId;
        expected.artifactId = pomAR.artifactId;
        expected.version = pomAR.version;
        assertEquals(expected, actual);
    }

    //TODO implement vulnerability inserter in the integration tests plugin and use that as a
    // dependency to automate the vulnerability insertion
    @Disabled("This is an integration test that checks if all the steps of vul-chain-finder work " +
        "correctly. It is only for local development and debugging. It requires DC up and running" +
        "with synthetic app:0.0.1 ingested, vulnerability inserted to wash method, and CI_URL " +
        "constant available.")
    @Test
    void testProcess() throws RocksDBException, IOException {
        final var id = getMavenId("eu.fasten-project.tests.syntheticjars", "app", "0.0.1");
        final Main main = setUpMainFor(id);

        main.process();

        final var actualStr = readResourceIntoString(createResourceNameFromID(id));
        final var expectedStr = readResourceIntoString("expected.json");

        JSONAssert.assertEquals(expectedStr, actualStr, JSONCompareMode.LENIENT);
    }

    private String createResourceNameFromID(final MavenId id) {
        return id.groupId + "-" + id.artifactId + "-" + id.version + ".json";
    }

    private Main setUpMainFor(final MavenId id) throws RocksDBException, FileNotFoundException {
        final var dbContext = DSL.using(DB_URL, USR, "fasten1234");
        final var om = new ObjectMapper().registerModule(new FastenURIJacksonModule());
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final var jsonUtils = new JsonUtilsImpl(om);
        final var db = new DatabaseUtils(dbContext, jsonUtils);
        final var ci = new RocksDao(CI_URL, true);
        final var resolver = new RestAPIDependencyResolver(LOCAL_REST, HTTP_CLIENT);
        final var repo = new VulnerableCallChainRepository(VUL_REPO.toString());
        final var main = new Main(db, ci, mock(Kafka.class), new VulChainFinderArgs(),
            mock(MessageGenerator.class), resolver, repo);
        main.setCurId(id);
        return main;
    }

    private String readResourceIntoString(final String name) throws IOException {
        final var path = Paths.get(VUL_REPO.toAbsolutePath().toString(), name);
        return Files.readString(path);
    }

    private MavenId getMavenId(final String g, final String a, final String v) {
        final var id = new MavenId();
        id.groupId = g;
        id.artifactId = a;
        id.version = v;
        return id;
    }
}