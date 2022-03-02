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

import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.vulchainfinder.utils.CallableIndexUtils;
import eu.f4sten.vulchainfinder.utils.DatabaseUtils;
import eu.f4sten.vulchainfinder.utils.JsonUtils;
import eu.fasten.core.data.callableindex.RocksDao;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;

class MainTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/fasten_java";

    private static final String PG_PWD = System.getenv("PG_PWD");
    private static final String USR = "fasten";
    private static final String G = "g";
    private static final String A = "a";
    private static final String V = "v";
    private static final MavenId EXPECTED = new MavenId();

    @Test
    void testExtractMavenIdFrom(){
        var pomAR = new PomAnalysisResult();
        pomAR.groupId = G;
        pomAR.artifactId = A;
        pomAR.version = V;
        var actual = extractMavenIdFrom(pomAR);
        EXPECTED.groupId = G;
        EXPECTED.artifactId = A;
        EXPECTED.version = V;
        assertEquals(EXPECTED, actual);
    }

    @Disabled
    @Test
    void testProcess() throws RocksDBException, IOException {
        final String CI_URL = "/Users/mehdi/Desktop/MyMac/TUD/FASTEN/Repositories/MainRepo/fasten-docker-deployment/docker-volumes/fasten/java/callable-index";
        var dbContext = DSL.using(DB_URL, USR, PG_PWD);
        var db = new DatabaseUtils(dbContext, new JsonUtils());
        var ci = new CallableIndexUtils(new RocksDao(CI_URL, true));
        var kafka = mock(Kafka.class);
        var args = new VulChainFinderArgs();
        args.vulnChainRepoUrl = "/Users/mehdi/Desktop/MyMac/TUD/FASTEN/Repositories/MainRepo/dataProcessing/data-processing/plugins/vulnerable-chain-finder/src/test/resources/vulrepo";
        args.restApiBaseURL = "https://api.fasten-project.eu";
        var msg = mock(MessageGenerator.class);
        var main = new Main(db, ci, kafka, args, msg);
        main.setCurId(EXPECTED);

        main.process();

        var actual = Files.readString(Path.of(""));
        var expected = "";

        assertEquals(expected, actual);
    }
}