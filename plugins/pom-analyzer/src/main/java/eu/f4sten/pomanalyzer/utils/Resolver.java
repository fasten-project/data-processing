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
package eu.f4sten.pomanalyzer.utils;

import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.COMPILE;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.PROVIDED;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.RUNTIME;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.SYSTEM;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenResolvedArtifactImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.pomanalyzer.data.NoArtifactRepositoryException;
import eu.f4sten.pomanalyzer.data.ResolutionResult;
import eu.f4sten.pomanalyzer.data.UnresolvablePomFileException;

public class Resolver {

    // Attention: Be aware that the test suite for this class is disabled by default
    // to avoid unnecessary downloads on every build. Make sure to re-enable the
    // tests and run them locally for every change in this class.

    public static final Logger LOG = LoggerFactory.getLogger(Resolver.class);

    public Set<ResolutionResult> resolveDependenciesFromPom(File pom) {
        var coordToResult = new HashMap<String, ResolutionResult>();

        resolvePom(pom).forEach(res -> {
            if (res.artifactRepository.startsWith("http")) {
                coordToResult.put(res.coordinate, res);
                return;
            }
            throw new NoArtifactRepositoryException(res.coordinate);
        });

        return new HashSet<>(coordToResult.values());
    }

    private static Set<ResolutionResult> resolvePom(File f) {
        var res = new HashSet<String[]>();
        try {
            MavenResolvedArtifactImpl.artifactRepositories = res;
            Maven.configureResolver() //
                    .withClassPathResolution(false) //
                    .loadPomFromFile(f) //
                    .importDependencies(COMPILE, RUNTIME, PROVIDED, SYSTEM) //
                    .resolve() //
                    .withTransitivity() //
                    .asResolvedArtifact();
            MavenResolvedArtifactImpl.artifactRepositories = null;
            return toResolutionResult(res);
        } catch (IllegalArgumentException e) {
            // no dependencies are declared, so no resolution required
            return new HashSet<>();
        }
    }

    private static Set<ResolutionResult> toResolutionResult(Set<String[]> res) {
        return res.stream() //
                .map(a -> {
                    if (!a[1].endsWith("/")) {
                        a[1] += "/";
                    }
                    return new ResolutionResult(a[0], a[1]);
                }) //
                .collect(Collectors.toSet());
    }

    public void resolveIfNotExisting(ResolutionResult artifact) {
        if (artifact.localPomFile.exists()) {
            LOG.info("Found artifact in .m2 folder: {} ({})", artifact.coordinate, artifact.artifactRepository);
            return;
        }
        LOG.info("Resolving/downloading POM file that does not exist in .m2 folder ...");
        resolvePom(artifact);
        if (!artifact.localPomFile.exists()) {
            throw new UnresolvablePomFileException(artifact.toString());
        }
    }

    private void resolvePom(ResolutionResult artifact) {
        var repoName = String.format("%s.%d", Resolver.class.getName(), new Date().getTime());
        Maven.configureResolver() //
                .withClassPathResolution(false) //
                .withMavenCentralRepo(false) //
                .withRemoteRepo(repoName, artifact.artifactRepository, "default") //
                .resolve(artifact.coordinate.replace("?", "pom")) //
                .withoutTransitivity() //
                .asResolvedArtifact();
    }
}