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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.f4sten.infra.impl.json.JsonUtilsImpl;
import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.vulchainfinder.json.FastenURIJacksonModule;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.data.metadatadb.codegen.tables.Callables;
import eu.fasten.core.data.metadatadb.codegen.tables.Modules;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import eu.fasten.core.data.vulnerability.Vulnerability;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSelectStep;
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
    public static String VUL_RECORD;
    public static DSLContext CONTEXT;
    public static DatabaseUtils DB;

    public static Map<FastenURI, List<Vulnerability>> EXPECTED_VULNERABILITY;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        CONTEXT = mock(DSLContext.class);
        var om = new ObjectMapper();
        om.registerModule(new FastenURIJacksonModule());
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final var jutils = new JsonUtilsImpl(om);
        DB = new DatabaseUtils(CONTEXT, jutils);
        VUL_RECORD = " \"CVE-2017-3164\": {\n" +
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
                "    }\n";

        var vulString = "{\n" +
            "  \"vulnerabilities\": {\n" +
            VUL_RECORD +
            "  }\n" +
            "}";

        final var setType = new TRef<HashMap<String, HashMap<String, Vulnerability>>>() {
        };

        HashMap<String, HashMap<String, Vulnerability>> VUL_ID_VUL_OBJECT_MAP =
            om.readValue(vulString, setType);

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
    void testSelectAllModulesOfIntegration() {
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
    void testSelectVulCallablesOfIntegration() {
        final var actual = getDb().selectVulCallablesOf(2);
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Disabled(SEE_WHY)
    @Test
    void testSelectVulCallablesOf2Integration() {
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

    @Test
    void testSelectAllModulesOf(){
        final var expected = Set.of(1L, 2L, 3L);
        setUpModuleSelection(expected);

        assertEquals(expected, DB.selectAllModulesOf(1));
    }

    @Test
    void testSelectVulCallablesOfWithNullVulField(){
        setUpModuleSelection(Set.of(1L));
        setUpCallableSelection(null);

        final var actual = DB.selectVulCallablesOf(Set.of(1L));

        assertEquals(Collections.emptyMap(), actual);
    }

    @Test
    void testSelectVulCallablesOfWithModuleEmpty(){
        setUpModuleSelection(Set.of());
        setUpCallableSelection(null);

        final var actual = DB.selectVulCallablesOf(Set.of(1L));

        assertEquals(Collections.emptyMap(), actual);
    }

    @Test
    void testSelectVulCallablesOfWithModuleNull(){
        setUpModuleSelection(null);
        setUpCallableSelection(null);

        final var actual = DB.selectVulCallablesOf(Set.of(1L));

        assertEquals(Collections.emptyMap(), actual);
    }

    @Test
    void testSelectVulCallablesOf(){
        setUpModuleSelection(Set.of(1L));
        setUpCallableSelection(JSONB.valueOf("{" + VUL_RECORD + "}"));

        final var actual = DB.selectVulCallablesOf(Set.of(1L));

        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    private void setUpModuleSelection(Set<Long> expected) {
        final var select = mock(SelectSelectStep.class);
        final var selectJoin = mock(SelectJoinStep.class);
        final var selectCondition = mock(SelectConditionStep.class);
        final var result = mock(Result.class);
        when(select.from(Modules.MODULES)).thenReturn(selectJoin);
        when(CONTEXT.select(Modules.MODULES.ID)).thenReturn(select);
        when(selectJoin.where(Modules.MODULES.PACKAGE_VERSION_ID.eq(1L))).thenReturn(selectCondition);
        when(selectCondition.fetch()).thenReturn(result);
        when(result.intoSet(Modules.MODULES.ID)).thenReturn(expected);
    }

    private void setUpCallableSelection(final JSONB vul) {
        DSLContext create = DSL.using(SQLDialect.DEFAULT);
        final var pn = Packages.PACKAGES.PACKAGE_NAME;
        final var v = PackageVersions.PACKAGE_VERSIONS.VERSION;
        final var uri = Callables.CALLABLES.FASTEN_URI;
        final var result = create.newResult();
        final var record =
            create.newRecord(pn, v, uri, Callables.CALLABLES.METADATA).values(
                "eu.fasten-project.tests.syntheticjars:lib",
                "0.0.1", "/lib/VehicleWash.wash(MotorVehicle)%2Fjava.lang%2FVoidType", vul);
        result.add(record);
        when(CONTEXT.fetch(anyString())).thenReturn(result);
    }

    private DatabaseUtils getDb() {
        final var om = new ObjectMapper().registerModule(new FastenURIJacksonModule());
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final var jsonUtils = new JsonUtilsImpl(om);
        return new DatabaseUtils(DSL.using(DB_URL, USR, PG_PWD), jsonUtils);
    }

}