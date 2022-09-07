package eu.f4sten.vulchainfinderdev.utils;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.vulchainfinderdev.exceptions.RestApiError;
import eu.fasten.core.data.metadatadb.codegen.tables.Dependencies;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.fasten.core.maven.data.ResolvedRevision;
import eu.fasten.core.maven.data.VersionConstraint;
import eu.fasten.core.maven.resolution.ResolverConfig;
import eu.fasten.core.maven.resolution.RestMavenResolver;
import org.jgrapht.alg.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

public class DependencyResolver {
    private static final String DEPS_ENDPOINT;
    public static final String PACKAGE_VERSION_ENDPOINT =
        "/packages/{groupId}:{artifactId}/{version}";

    static {
        DEPS_ENDPOINT = PACKAGE_VERSION_ENDPOINT + "/deps";
    }

    private static final int OK = 200;
    private final String restAPIBaseURL;
    private final String depResolverBaseURL;
    private final HttpClient client;

    public String getRestAPIBaseURL() {
        return restAPIBaseURL;
    }

    public DependencyResolver(String restAPIBaseURL, String depResolverBaseURL, HttpClient client) {
        this.restAPIBaseURL = restAPIBaseURL;
        this.client = client;
        this.depResolverBaseURL = depResolverBaseURL;
    }

    public Set<Long> resolveDependencyIds(final MavenId id) {
        final HttpResponse<String> response = requestEndPoint(DEPS_ENDPOINT, id);
        final var depIds = extractPackageIdsFromResponse(response);
        depIds.add(extractPackageVersionId(id));
        return depIds;
    }

    public Set<Pair<Long, Pair<MavenId, File>>> resolveDependencies(final MavenId id, final String m2Path) {
        var restMavenResolver = new RestMavenResolver(depResolverBaseURL);
        var restMavenResolverConfig = new ResolverConfig();
        var deps = restMavenResolver.resolveDependencies(List.of(id.asCoordinate()), restMavenResolverConfig);

        final Set<Pair<Long, Pair<MavenId, File>>> depsPair = new HashSet<>();
        for (var d: deps) {
            var mvnId = extractMavenIDsFromDGR(d);
            // TODO: Use the DB to find pkg. version IDs rather than a REST API call
            depsPair.add(new Pair<>(extractPackageVersionId(mvnId), new Pair<>(mvnId,
                    new File(Paths.get(m2Path, mvnId.toJarPath()).toString()))));
        }
        return depsPair;
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

    public long extractPackageVersionId(final MavenId id) {
        final HttpResponse<String> appResponse = requestEndPoint(PACKAGE_VERSION_ENDPOINT, id);
        final var fieldName = PackageVersions.PACKAGE_VERSIONS.ID.getName();
        return extractLongFieldFromJSONObj(new JSONObject(appResponse.body()), fieldName);
    }

//    public Set<Pair<String, Path>> extractMavenIdsFromResponse(final HttpResponse<String> response) {
//        final Set<Pair<String, Path>> result = new HashSet<>();
//
//        final var deps = new JSONArray(response.body());
//        for (final var dep : deps) {
//            final var fieldName = Dependencies.DEPENDENCIES.DEPENDENCY_ID.getName();
//            JSONObject depMetadata = (JSONObject) ((JSONObject) dep).get("metadata");
//            var mvnId = extractMavenIdsFromMetadata(depMetadata);
//            result.add(new Pair<>(mvnId.asCoordinate(), mvnId.toJarPath()));
//        }
//        return result;
//    }

    public MavenId extractMavenIdsFromMetadata(JSONObject depMetadata) {
        var gId = (String) depMetadata.get("groupId");
        var aID = (String) depMetadata.get("artifactId");
        var verConst = new VersionConstraint((String) ((JSONArray) depMetadata.get("versionConstraints")).get(0));
        var type = (String) depMetadata.get("type");
        // We select the upper bound for dependency versions
        return new MavenId(gId, aID, verConst.getUpperBound().trim(), null, type);
    }

    public MavenId extractMavenIDsFromDGR(ResolvedRevision revision) {
        return new MavenId(revision.getGroupId(), revision.getArtifactId(), revision.version.toString(),
                null, "jar");
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
            throw new RestApiError("Problem requesting Rest API.", exception);
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
