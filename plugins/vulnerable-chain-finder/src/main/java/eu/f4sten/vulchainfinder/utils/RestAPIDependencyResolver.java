package eu.f4sten.vulchainfinder.utils;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.metadatadb.codegen.tables.Dependencies;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class RestAPIDependencyResolver {
    private static final String DEPS_ENDPOINT;
    public static final String PACKAGE_VERSION_ENDPOINT =
        "/api/mvn/packages/{groupId}:{artifactId}/{version}";

    static {
        DEPS_ENDPOINT = PACKAGE_VERSION_ENDPOINT + "/deps";
    }

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

    public Set<Long> resolveDependencyIds(final MavenId id) {
        final HttpResponse<String> response = requestEndPoint(DEPS_ENDPOINT, id);
        final var depIds = extractPackageIdsFromResponse(response);
        final HttpResponse<String> appResponse = requestEndPoint(PACKAGE_VERSION_ENDPOINT, id);
        final var fieldName = PackageVersions.PACKAGE_VERSIONS.ID.getName();
        depIds.add(extractLongFieldFromJSONObj(new JSONObject(appResponse.body()), fieldName));
        return depIds;
    }

    private HttpResponse<String> requestEndPoint(final String depsEndpoint, final MavenId id) {
        final var uri = createUri(depsEndpoint, id);
        final var request = HttpRequest.newBuilder().uri(uri).GET().build();
        return sendOrThrow(request);
    }

    public long extractLongFieldFromJSONObj(final JSONObject response, final String field) {
        return (int) response.get(field);
    }

    public Set<Long> extractPackageIdsFromResponse(final HttpResponse<String> response) {
        final Set<Long> result = new HashSet<>();

        final var deps = new JSONArray(response.body());
        for (final var dep : deps) {
            final var fieldName = Dependencies.DEPENDENCIES.DEPENDENCY_ID.getName();
            long id = extractLongFieldFromJSONObj((JSONObject) dep, fieldName);
            result.add(id);
        }
        return result;
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

    public URI createUri(final String endpoint, final MavenId id) {
        return returnFullUriOrThrow(placeIDInEndpoint(endpoint, id));
    }

    public String placeIDInEndpoint(final String uri, final MavenId id) {
        return uri
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
