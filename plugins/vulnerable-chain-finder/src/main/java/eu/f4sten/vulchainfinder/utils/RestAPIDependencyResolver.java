package eu.f4sten.vulchainfinder.utils;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.metadatadb.codegen.tables.Dependencies;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import kotlin.collections.ArrayDeque;
import org.json.JSONArray;
import org.json.JSONObject;

public class RestAPIDependencyResolver {
    private static final String ENDPOINT = "/api/mvn/packages/{groupId}:{artifactId}/{version}/deps";
    private static final int OK = 200;
    private final String restAPIBaseURL;
    private final HttpClient client;

    public String getRestAPIBaseURL() {
        return restAPIBaseURL;
    }

    public RestAPIDependencyResolver(String restAPIBaseURL, HttpClient client) {
        this.restAPIBaseURL = restAPIBaseURL;
        this.client = client;
    }

    public List<Long> resolveDependencyIds(final MavenId id) {
        final var uri = createDepResolverUri(id);
        final var request = HttpRequest.newBuilder().uri(uri).GET().build();
        final var response = sendOrThrow(request);
        return extractPackageIdsFromResponse(response);
    }

    public List<Long> extractPackageIdsFromResponse(final HttpResponse<String> response) {
        final List<Long> result = new ArrayDeque<>();

        final var deps = new JSONArray(response.body());
        for (final var dep : deps) {
            long id = extractLongIdFromJsonObject((JSONObject) dep);
            result.add(id);
        }

        return result;
    }

    public long extractLongIdFromJsonObject(final JSONObject dep) {
        final var fieldName = Dependencies.DEPENDENCIES.DEPENDENCY_ID.getName();
        return (int) dep.get(fieldName);
    }

    public static boolean isNotOK(final HttpResponse<String> response) {
        return response.statusCode() != OK;
    }

    public HttpResponse<String> sendOrThrow(final HttpRequest request) {
        HttpResponse<String> response = null;
        Exception exception = null;

        try {
             response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            exception = e;
        }
        if (exception != null || response == null || isNotOK(response)) {
            throw new RuntimeException("Problem requesting Rest API.", exception);
        }

        return response;
    }

    public URI createDepResolverUri(final MavenId id) {
        return returnFullUriOrThrow(placeIDInEndpoint(id));
    }

    public String placeIDInEndpoint(final MavenId id) {
        return ENDPOINT
            .replace("{groupId}", id.groupId)
            .replace("{artifactId}", id.artifactId)
            .replace("{version}", id.version);
    }

    public URI returnFullUriOrThrow(final String endpoint) {
        try {
            return new URI(makeCorrectFullURI(endpoint));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String makeCorrectFullURI(String endpoint) {
        var delim = "/";
        if (endpoint == null || this.restAPIBaseURL == null) {
            return null;
        }
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        String base = this.restAPIBaseURL;
        if (this.restAPIBaseURL.endsWith("/")) {
            base = base.replaceFirst(".$", "");
        }

        return String.format("%s%s%s", base, delim, endpoint);
    }
}
