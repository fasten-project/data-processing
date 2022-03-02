package eu.f4sten.vulchainfinder.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.f4sten.pomanalyzer.data.MavenId;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RestAPIDependencyResolverTest {

    public static String BASE_URL;
    public static JSONObject DEP_JSON;
    public static JSONArray DEPS_JSON_ARRAY;
    public static MavenId ID;
    public static HttpResponse MOCK_RESPONSE;
    public static HttpClient MOCK_CLIENT;
    public static String EXPECTED_URI_STRING;
    public static String ENDPOINT_WITH_SLASH;
    public static String ENDPOINT_WITHOUT_SLASH;
    public static RestAPIDependencyResolver RESOLVER_WITHOUT_SLASH;
    public static RestAPIDependencyResolver RESOLVER_WITH_SLASH;

    @BeforeEach
    void setUp() {
        BASE_URL = "https://api.fasten-project.eu";
        DEP_JSON = new JSONObject("  {\n" +
            "    \"package_version_id\": 1,\n" +
            "    \"dependency_id\": 2" +
            "  }");
        DEPS_JSON_ARRAY = new JSONArray();
        DEPS_JSON_ARRAY.put(DEP_JSON);

        ID = new MavenId();
        ID.groupId = "org.apache.solr";
        ID.artifactId = "solr-core";
        ID.version = "6.6.1";

        MOCK_CLIENT = mock(HttpClient.class);
        MOCK_RESPONSE = mock(HttpResponse.class);

        ENDPOINT_WITHOUT_SLASH = "api/mvn/packages/org.apache.solr:solr-core/6.6.1/deps";
        ENDPOINT_WITH_SLASH = "/api/mvn/packages/org.apache.solr:solr-core/6.6.1/deps";
        RESOLVER_WITHOUT_SLASH = new RestAPIDependencyResolver(BASE_URL, MOCK_CLIENT);
        RESOLVER_WITH_SLASH = new RestAPIDependencyResolver(BASE_URL, MOCK_CLIENT);

        EXPECTED_URI_STRING = RESOLVER_WITHOUT_SLASH.getRestAPIBaseURL() + ENDPOINT_WITH_SLASH;
    }

    @Disabled("Depends on the Rest Api and current state of the DB." +
        "Run while development and adjust accordingly")
    @Test
    void resolveServer() {
        var resolver = new RestAPIDependencyResolver(BASE_URL, HttpClient.newBuilder().build());
        var actual = resolver.resolveDependencyIds(ID);
        assertEquals(List.of(4L, 8L, 42L, 53L, 55L, 56L, 59L, 70L, 71L, 159L), actual);
    }

    @Disabled("Works with Docker Compose, only when synthetic jar app is inserted!" +
        "Run while development and adjust accordingly")
    @Test
    void resolveLocal() {
        var resolver =
            new RestAPIDependencyResolver("http://localhost:9080", HttpClient.newBuilder().build());
        var id = new MavenId();
        id.groupId = "eu.fasten-project.tests.syntheticjars";
        id.artifactId = "app";
        id.version = "0.0.1";
        var actual = resolver.resolveDependencyIds(id);
        assertEquals(List.of(2L), actual);
    }

    @Test
    void testExtractPackageIdsFromResponse() {
        when(MOCK_RESPONSE.body()).thenReturn(DEPS_JSON_ARRAY.toString());
        var actual = RESOLVER_WITH_SLASH.extractPackageIdsFromResponse(MOCK_RESPONSE);
        assertEquals(Set.of(2L), actual);
    }

    @Test
    void testExtractLongIdFromJsonObject() {
        final var actual = RESOLVER_WITH_SLASH.extractLongIdFromJsonObject(DEP_JSON);
        assertEquals(2L, actual);
    }

    @Test
    void testIsOk() {
        when(MOCK_RESPONSE.statusCode()).thenReturn(200);
        assertFalse(RestAPIDependencyResolver.isNotOK(MOCK_RESPONSE));
    }

    @Test
    void testIsNotOK() {
        when(MOCK_RESPONSE.statusCode()).thenReturn(400);
        assertTrue(RestAPIDependencyResolver.isNotOK(MOCK_RESPONSE));
    }

    @Test
    void testSendOrThrowCallsSend() throws IOException, InterruptedException {
        when(MOCK_CLIENT.send(any(), any())).thenReturn(any());
        try {
            RESOLVER_WITH_SLASH.sendOrThrow(any());
        } catch (RuntimeException ignored) {
            // This catch is ignored. It only helps us check if the send method is called
            System.out.println();
        }
        verify(MOCK_CLIENT, times(1)).send(any(), any());
    }

    @Test
    void placeIDInEndpoint() {
        var actual = RESOLVER_WITH_SLASH.placeIDInEndpoint(ID);
        assertEquals(ENDPOINT_WITH_SLASH, actual);
    }

    @Test
    void testReturnFullUri() throws URISyntaxException {
        var actual = RESOLVER_WITH_SLASH.returnFullUriOrThrow(ENDPOINT_WITH_SLASH);
        assertEquals(actual, new URI(EXPECTED_URI_STRING));
    }

    @Test
    void testThrowsURISyntaxException() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            RESOLVER_WITH_SLASH.returnFullUriOrThrow(ENDPOINT_WITH_SLASH + "[]");
        });

        var expectedMessage = "Illegal character in path at index 83";
        var actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        assertTrue(exception.getCause() instanceof URISyntaxException);
    }

    @Test
    void testThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            RESOLVER_WITH_SLASH.returnFullUriOrThrow(null);
        });
    }

    @Test
    void testMakeCorrectFullURI1() {
        final var actual = RESOLVER_WITHOUT_SLASH.makeCorrectFullURI(ENDPOINT_WITH_SLASH);
        assertEquals(EXPECTED_URI_STRING, actual);
    }

    @Test
    void testMakeCorrectFullURI2() {
        final var actual = RESOLVER_WITHOUT_SLASH.makeCorrectFullURI(ENDPOINT_WITHOUT_SLASH);
        assertEquals(EXPECTED_URI_STRING, actual);
    }

    @Test
    void testMakeCorrectFullURI3() {
        final var actual = RESOLVER_WITH_SLASH.makeCorrectFullURI(ENDPOINT_WITH_SLASH);
        assertEquals(EXPECTED_URI_STRING, actual);
    }

    @Test
    void testMakeCorrectFullURI4() {
        final var actual = RESOLVER_WITH_SLASH.makeCorrectFullURI(ENDPOINT_WITHOUT_SLASH);
        assertEquals(EXPECTED_URI_STRING, actual);
    }

}