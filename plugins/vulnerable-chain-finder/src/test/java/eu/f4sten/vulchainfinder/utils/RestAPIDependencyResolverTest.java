package eu.f4sten.vulchainfinder.utils;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.metadatadb.codegen.tables.Dependencies;

class RestAPIDependencyResolverTest {

    private static final HttpResponse SOME_RESPONSE = mock(HttpResponse.class);
    private static final HttpRequest SOME_REQUEST = mock(HttpRequest.class);
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
    void setUp() throws JSONException {
        BASE_URL = "https://api.fasten-project.eu";
        DEP_JSON = new JSONObject("  {\n" + "    \"package_version_id\": 1,\n" + "    \"dependency_id\": 2" + "  }");
        DEPS_JSON_ARRAY = new JSONArray();
        DEPS_JSON_ARRAY.put(DEP_JSON);

        ID = new MavenId();
        ID.groupId = "org.apache.solr";
        ID.artifactId = "solr-core";
        ID.version = "6.6.1";

        MOCK_CLIENT = mock(HttpClient.class);
        MOCK_RESPONSE = mock(HttpResponse.class);

        // TODO break this up into multiple test cases

        ENDPOINT_WITHOUT_SLASH = "api/mvn/packages/org.apache.solr:solr-core/6.6.1/deps";
        ENDPOINT_WITH_SLASH = "/api/mvn/packages/org.apache.solr:solr-core/6.6.1/deps";
        RESOLVER_WITHOUT_SLASH = new RestAPIDependencyResolver(BASE_URL, MOCK_CLIENT);
        RESOLVER_WITH_SLASH = new RestAPIDependencyResolver(BASE_URL, MOCK_CLIENT);

        // TODO DO NOT call sut methods in your setup... your are supposed to test
        // these!
        EXPECTED_URI_STRING = RESOLVER_WITHOUT_SLASH.getRestAPIBaseURL() + ENDPOINT_WITH_SLASH;
    }

    @Disabled("Depends on the Rest Api and current state of the DB." + "Run while development and adjust accordingly")
    @Test
    void resolveServer() {
        var resolver = new RestAPIDependencyResolver(BASE_URL, HttpClient.newBuilder().build());
        var actual = resolver.resolveDependencyIds(ID);
        assertEquals(Set.of(4L, 8L, 42L, 53L, 55L, 56L, 59L, 70L, 71L, 159L, 10479L), actual);
    }

    @Disabled("Works with Docker Compose, only when synthetic jar app is inserted!"
            + "Run while development and adjust accordingly")
    @Test
    void resolveLocal() {
        var resolver = new RestAPIDependencyResolver("http://localhost:9080", HttpClient.newBuilder().build());
        var id = new MavenId();
        id.groupId = "eu.fasten-project.tests.syntheticjars";
        id.artifactId = "app";
        id.version = "0.0.1";
        var actual = resolver.resolveDependencyIds(id);
        assertEquals(Set.of(2L, 1L), actual);
    }

    @Test
    void testExtractPackageIdsFromResponse() {
        when(MOCK_RESPONSE.body()).thenReturn(DEPS_JSON_ARRAY.toString());
        var actual = RESOLVER_WITH_SLASH.extractPackageIdsFromResponse(MOCK_RESPONSE);
        assertEquals(Set.of(2L), actual);
    }

    @Test
    void testExtractLongFieldFromJSONObj() {
        final var actual = RESOLVER_WITH_SLASH.extractLongFieldFromJSONObj(DEP_JSON,
                Dependencies.DEPENDENCIES.DEPENDENCY_ID.getName());
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
    @SuppressWarnings("unchecked")
    void testSendOrThrowCallsSend() throws IOException, InterruptedException {
        when(SOME_RESPONSE.statusCode()).thenReturn(SC_OK);
        // TODO Urgently clarify use of matchers!
        when(MOCK_CLIENT.send(any(), any())).thenReturn(SOME_RESPONSE);
        RESOLVER_WITH_SLASH.sendOrThrow(SOME_REQUEST);
        verify(MOCK_CLIENT, times(1)).send(any(), any());
    }

    @Test
    void placeIDInEndpoint() {
        var actual = RESOLVER_WITH_SLASH.placeIDInEndpoint(ENDPOINT_WITH_SLASH, ID);
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