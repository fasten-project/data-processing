package eu.f4sten.vulchainfinder.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.vulnerability.Vulnerability;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DatabaseUtilsTest {


    public static final String WHY_DISABLED =
        "Only for local development. It requires Docker Compose up and running and the " +
            "synthetic jars ingested, with inserted vulnerability in callable table.";
    public static Map<FastenURI, List<Vulnerability>> EXPECTED_VULNERABILITY;
    public static org.jooq.CloseableDSLContext CONTEXT;
    public static DatabaseUtils DB;

    @BeforeEach
    void setUp() {
        var vulString = "{\n" +
            "  \"vulnerabilities\": {\n" +
            "    \"CVE-2021-29262\": {\n" +
            "      \"id\": \"CVE-2021-29262\",\n" +
            "      \"cwe_ids\": [\n" +
            "        \"CWE-522\"\n" +
            "      ],\n" +
            "      \"severity\": \"MEDIUM\",\n" +
            "      \"scoreCVSS2\": 4.3,\n" +
            "      \"scoreCVSS3\": 7.5,\n" +
            "      \"vectorCVSS2\": \"AV:N/AC:M/Au:N/C:P/I:N/A:N\",\n" +
            "      \"vectorCVSS3\": \"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        Type setType =
            new TypeToken<HashMap<String, HashMap<String, Vulnerability>>>() {}.getType();
        Map<String, Map<String, Vulnerability>> VUL_ID_VUL_OBJECT_MAP =
            new JsonUtils().fromJson(vulString, setType);

        EXPECTED_VULNERABILITY =
                Map.of(FastenURI.create("fasten://mvn!eu.fasten-project.tests.syntheticjars:app$0.0" +
                        ".1/app/RoadTrip.%3Cinit%3E(%2Flib%2FVehicleWash)%2Fjava.lang%2FVoidType"),
                    List.of(VUL_ID_VUL_OBJECT_MAP.get("vulnerabilities").get("CVE-2021-29262")));


        CONTEXT = DSL.using("jdbc:postgresql://localhost:5432/fasten_java",
            "fasten", System.getenv("PG_PWD"));
        DB = new DatabaseUtils(CONTEXT, new JsonUtils());
    }

    @Disabled("Only for local development. It requires Docker Compose up and running and the " +
        "synthetic jars ingested.")
    @Test
    void testSelectAllModulesOf() {
        var actual = DB.selectAllModulesOf(1);
        assertEquals(Set.of(1L, 2L, 3L, 4L), actual);
    }

    @Test
    void testCreateFastenUri(){
        final var mockRecord = mock(Record.class);
        when(mockRecord.get(0)).thenReturn("eu.fasten-project.tests.syntheticjars:app");
        when(mockRecord.get(1)).thenReturn("0.0.1");
        when(mockRecord.get(2)).thenReturn("/app/RoadTrip.%3Cinit%3E(%2Flib%2FVehicleWash)%2Fjava.lang%2FVoidType");
        final var actual = DB.createFastenUri(mockRecord);
        assertEquals(FastenURI.create("fasten://mvn!eu.fasten-project.tests.syntheticjars:app$0.0" +
            ".1/app/RoadTrip.%3Cinit%3E(%2Flib%2FVehicleWash)%2Fjava.lang%2FVoidType"), actual);
    }

    @Test
    void testSelectVulCallablesWhereModuleIdIs(){
        final var actual = DB.selectVulCallablesWhereModuleIdIs(1L);
        assertTrue(actual.endsWith("and callables.module_id = 1"));
    }

    @Disabled(WHY_DISABLED)
    @Test
    void testSelectConcurrentlyVulCallablesOf(){
        final var actual = DB.selectConcurrentlyVulCallablesOf(Set.of(1L));
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Disabled(WHY_DISABLED)
    @Test
    void testSelectVulCallablesOf(){
        final var actual = DB.selectVulCallablesOf(1);
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }

    @Disabled(WHY_DISABLED)
    @Test
    void testSelectVulCallablesOf2(){
        final var actual = DB.selectVulCallablesOf(Set.of(1L));
        assertEquals(EXPECTED_VULNERABILITY, actual);
    }


}