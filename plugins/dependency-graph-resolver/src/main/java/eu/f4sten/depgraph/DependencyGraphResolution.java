/*
 * Copyright 2022 Delft University of Technology
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
package eu.f4sten.depgraph;

import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fasten.core.maven.data.Scope;
import eu.fasten.core.maven.resolution.IMavenResolver;
import eu.fasten.core.maven.resolution.MavenResolutionException;
import eu.fasten.core.maven.resolution.ResolverConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/depgraph")
public class DependencyGraphResolution {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyGraphResolution.class);

    private IMavenResolver resolver;

    @Inject
    public DependencyGraphResolution(IMavenResolver resolver) {
        this.resolver = resolver;
    }

    @GET
    @Path("/dependents/{groupId}/{artifactId}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveDependents( //
            @PathParam("groupId") String groupId, //
            @PathParam("artifactId") String artifactId, //
            @PathParam("version") String version, //
            @QueryParam("resolveAt") Long resolveAt, //
            @QueryParam("depth") String depth, //
            @QueryParam("limit") Integer limit, //
            @QueryParam("scope") Scope scope, //
            @QueryParam("alwaysIncludeProvided") Boolean alwaysIncludeProvided, //
            @QueryParam("alwaysIncludeOptional") Boolean alwaysIncludeOptional) {

        var config = getConfig(resolveAt, depth, limit, scope, alwaysIncludeProvided, alwaysIncludeOptional);
        try {
            var dpts = resolver.resolveDependents(groupId, artifactId, version, config);
            return Response.ok(dpts).build();
        } catch (MavenResolutionException e) {
            logError("resolveDependents", Set.of(gav(groupId, artifactId, version)), config, e);
            return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY, e.getMessage()).build();
        } catch (Exception e) {
            logError("resolveDependents", Set.of(gav(groupId, artifactId, version)), config, e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()).build();
        }
    }

    private static String gav(String groupId, String artifactId, String version) {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }

    @POST
    @Path("/dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveDependencies( //
            Set<String> gavs, //
            @QueryParam("resolveAt") Long resolveAt, //
            @QueryParam("depth") String depth, //
            @QueryParam("limit") Integer limit, //
            @QueryParam("scope") Scope scope, //
            @QueryParam("alwaysIncludeProvided") Boolean alwaysIncludeProvided, //
            @QueryParam("alwaysIncludeOptional") Boolean alwaysIncludeOptional) {

        var config = getConfig(resolveAt, depth, limit, scope, alwaysIncludeProvided, alwaysIncludeOptional);
        try {
            var deps = resolver.resolveDependencies(gavs, config);
            return Response.ok(deps).build();
        } catch (MavenResolutionException e) {
            logError("resolveDependencies", gavs, config, e);
            return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY, e.getMessage()).build();
        } catch (Exception e) {
            logError("resolveDependencies", gavs, config, e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()).build();
        }
    }

    private static ResolverConfig getConfig(Long resolveAt, String depth, Integer limit, Scope scope,
            Boolean alwaysIncludeProvided, Boolean alwaysIncludeOptional) {
        var cfg = new ResolverConfig();
        if (resolveAt != null) {
            cfg.resolveAt = resolveAt;
        }
        if (depth != null) {
            parseDepth(depth, n -> cfg.depth = n);
        }
        if (limit != null) {
            parseLimit(limit, n -> cfg.limit = n);
        }
        if (scope != null) {
            cfg.scope = scope;
        }
        if (alwaysIncludeProvided != null) {
            cfg.alwaysIncludeProvided = alwaysIncludeProvided;
        }
        if (alwaysIncludeOptional != null) {
            cfg.alwaysIncludeOptional = alwaysIncludeOptional;
        }
        return cfg;
    }

    private static void parseDepth(String depth, Consumer<Integer> c) {
        depth = depth.toUpperCase().strip();

        if ("MAX".equals(depth) || "TRANSITIVE".equals(depth)) {
            c.accept(Integer.MAX_VALUE);
        } else if ("DIRECT".equals(depth)) {
            c.accept(1);
        } else {
            try {
                var n = Integer.parseInt(depth);
                if (n < 1) {
                    var msg = "Ignoring invalid depth (%d)";
                    LOG.error(String.format(msg, n));
                    return;
                }
                c.accept(n);
            } catch (NumberFormatException e) {
                var msg = "Ignoring unparseable depth (%s)";
                LOG.error(String.format(msg, depth), e);
            }
        }
    }

    private static void parseLimit(int limit, Consumer<Integer> c) {
        if (limit < 1) {
            var msg = "Ignoring invalid limit (%d)";
            LOG.error(String.format(msg, limit));
            return;
        }
        c.accept(limit);
    }

    private static void logError(String endpoint, Set<String> gavs, ResolverConfig cfg, Exception e) {
        var msg = "%s in %s(%s, %s):";
        LOG.error(String.format(msg, e.getClass().getSimpleName(), endpoint, gavs, cfg), e);
    }
}