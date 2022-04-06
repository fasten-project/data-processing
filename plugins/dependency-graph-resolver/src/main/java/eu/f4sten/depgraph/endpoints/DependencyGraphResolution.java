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
package eu.f4sten.depgraph.endpoints;

import java.util.Set;

import javax.inject.Inject;

import eu.fasten.core.maven.data.Revision;
import eu.fasten.core.maven.data.Scope;
import eu.fasten.core.maven.resolution.IMavenResolver;
import eu.fasten.core.maven.resolution.ResolverConfig;
import eu.fasten.core.maven.resolution.ResolverDepth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/depgraph")
public class DependencyGraphResolution {

    private IMavenResolver resolver;

    @Inject
    public DependencyGraphResolution(IMavenResolver resolver) {
        this.resolver = resolver;
    }

    // TODO return a "Response" that allows us to add error information

    @GET
    @Path("/dependents/{groupId}/{artifactId}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Revision> resolveDependents( //
            @PathParam("groupId") String groupId, //
            @PathParam("artifactId") String artifactId, //
            @PathParam("version") String version, //
            @QueryParam("resolveAt") Long resolveAt, //
            @QueryParam("depth") ResolverDepth depth, //
            @QueryParam("scope") Scope scope, //
            @QueryParam("alwaysIncludeProvided") Boolean alwaysIncludeProvided, //
            @QueryParam("alwaysIncludeOptional") Boolean alwaysIncludeOptional) {

        var config = getConfig(resolveAt, depth, scope, alwaysIncludeProvided, alwaysIncludeOptional);
        return resolver.resolveDependents(groupId, artifactId, version, config);
    }

    @POST
    @Path("/dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Revision> resolveDependencies( //
            Set<String> gavs, //
            @QueryParam("resolveAt") Long resolveAt, //
            @QueryParam("depth") ResolverDepth depth, //
            @QueryParam("scope") Scope scope, //
            @QueryParam("alwaysIncludeProvided") Boolean alwaysIncludeProvided, //
            @QueryParam("alwaysIncludeOptional") Boolean alwaysIncludeOptional) {

        var config = getConfig(resolveAt, depth, scope, alwaysIncludeProvided, alwaysIncludeOptional);
        return resolver.resolveDependencies(gavs, config);
    }

    private static ResolverConfig getConfig(Long resolveAt, ResolverDepth depth, Scope scope,
            Boolean alwaysIncludeProvided, Boolean alwaysIncludeOptional) {
        var cfg = new ResolverConfig();
        if (resolveAt != null) {
            cfg.resolveAt = resolveAt;
        }
        if (depth != null) {
            cfg.depth = depth;
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
}