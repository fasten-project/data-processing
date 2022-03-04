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

package eu.f4sten.vulchainfinder.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.data.vulnerability.Vulnerability;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DatabaseUtilsTest {

    public static final String SEE_WHY =
        "Only for local development. It requires Docker Compose up and running and the " +
            "synthetic jars ingested, with inserted vulnerability in callable table.";
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/fasten_java";
    public static final String PG_PWD = System.getenv("PG_PWD");
    public static final String USR = "fasten";
    public static final JsonUtils JSON_UTILS = new JsonUtils();

    public static Map<FastenURI, List<Vulnerability>> EXPECTED_VULNERABILITY;

    @BeforeEach
    void setUp() {
        var vulString ="{\n" +
            "  \"vulnerabilities\": {\n" +
            "    \"CVE-2017-3164\": {\n" +
            "      \"id\": \"CVE-2017-3164\",\n" +
            "      \"cwe_ids\": [\n" +
            "        \"CWE-918\"\n" +
            "      ],\n" +
            "      \"severity\": \"MEDIUM\",\n" +
            "      \"patch_date\": \"2019-01-15\",\n" +
            "      \"scoreCVSS2\": 5,\n" +
            "      \"scoreCVSS3\": 7.5,\n" +
            "      \"description\": \"Server Side Request Forgery in Apache Solr, versions 1.3 until 7.6 (inclusive). Since the \\\"shards\\\" parameter does not have a corresponding whitelist mechanism, a remote attacker with access to the server could make Solr perform an HTTP GET request to any reachable URL.\",\n" +
            "      \"vectorCVSS2\": \"AV:N/AC:L/Au:N/C:P/I:N/A:N\",\n" +
            "      \"vectorCVSS3\": \"CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N\",\n" +
            "      \"published_date\": \"2019-03-08\",\n" +
            "      \"last_modified_date\": \"2020-12-09\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        Type setType = new TypeToken<HashMap<String, HashMap<String, Vulnerability>>>() {
        }.getType();
        Map<String, Map<String, Vulnerability>> VUL_ID_VUL_OBJECT_MAP =
            new JsonUtils().fromJson(vulString, setType);

        EXPECTED_VULNERABILITY = Map.of(FastenURI.create(
                "fasten://mvn!eu.fasten-project.tests.syntheticjars:lib$0.0.1/lib/VehicleWash.wash(MotorVehicle)%2Fjava.lang%2FVoidType"),
            List.of(VUL_ID_VUL_OBJECT_MAP.get("vulnerabilities").get("CVE-2017-3164")));

    }

    @Test
    void testCreateFastenUri() {
        final var mockRecord = mock(Record.class);
        when(mockRecord.get(0)).thenReturn("eu.fasten-project.tests.syntheticjars:lib");
        when(mockRecord.get(1)).thenReturn("0.0.1");
        when(mockRecord.get(2)).thenReturn(
            "/lib/BasicMotorVehicle.addDirt()%2Fjava.lang%2FVoidType");
        final var actual = DatabaseUtils.createFastenUriFromPckgVersionUriFields(mockRecord);
        assertEquals(FastenURI.create("fasten://mvn!eu.fasten-project.tests.syntheticjars:lib$0.0" +
            ".1/lib/BasicMotorVehicle.addDirt()%2Fjava.lang%2FVoidType"), actual);
    }

    @Test
    void testSelectVulCallablesWhereModuleIdIs() {
        final var actual = DatabaseUtils.createStrForSelectVulCallablesWhereModuleIdIs(2L);
        assertTrue(actual.endsWith("and callables.module_id = 2"));
    }

    @Disabled(SEE_WHY)
    @Test
    void testSelectAllModulesOf() {
        var actual = getDb().selectAllModulesOf(1);
        assertEquals(Set.of(1L, 2L, 3L, 4L), actual);
    }

    @Disabled(SEE_WHY)
    @Test
    void testSelectConcurrentlyVulCallablesOf() {
        final var actual = getDb().selectConcurrentlyVulCallablesOf(Set.of(6L));
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Disabled(SEE_WHY)
    @Test
    void testSelectVulCallablesOf() {
        final var actual = getDb().selectVulCallablesOf(2);
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Disabled(SEE_WHY)
    @Test
    void testSelectVulCallablesOf2() {
        final var actual = getDb().selectVulCallablesOf(Set.of(2L));
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Test
    void testSelectVulnerablePackagesExistingIn() {
        var dao = mock(MetadataDao.class);
        var json = mock(JsonUtils.class);
        var dslContext = mock(DSLContext.class);
        var db = new DatabaseUtils(dslContext, json) {
            public MetadataDao getDao(DSLContext ctx) {
                return dao;
            }
        };
        final var depIds = Set.of(1L, 2L);
        db.selectVulnerablePackagesExistingIn(depIds);

        verify(dao).findVulnerablePackageVersions(depIds);
    }

    private DatabaseUtils getDb() {
        return new DatabaseUtils(DSL.using(DB_URL, USR, PG_PWD), JSON_UTILS);
    }

}