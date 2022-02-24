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

import static java.lang.String.format;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.COMPILE;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.PROVIDED;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.RUNTIME;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.SYSTEM;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenChecksumPolicy;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepositories;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenUpdatePolicy;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenResolvedArtifactImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.pomanalyzer.data.ResolutionResult;
import eu.f4sten.pomanalyzer.exceptions.NoArtifactRepositoryException;
import eu.f4sten.pomanalyzer.exceptions.UnresolvablePomFileException;

public class Resolver {

    // Attention: Be aware that the test suite for this class is disabled by default
    // to avoid unnecessary downloads on every build. Make sure to re-enable the
    // tests and run them locally for every change in this class.

    private static final Logger LOG = LoggerFactory.getLogger(Resolver.class);

    public Set<ResolutionResult> resolveDependenciesFromPom(File pom, String artifactRepository) {
        var coordToResult = new HashMap<String, ResolutionResult>();

        // two iterations: 0) resolving and (potential) deletion 1) get artifactRepos
        range(0, 2).forEach(i -> {
            resolvePom(pom, artifactRepository).forEach(res -> {
                // ignore known dependencies or those that should be skipped (e.g., exist in DB)
                if (coordToResult.containsKey(res.coordinate)) {
                    return;
                }

                // remember identified artifactRepository
                if (res.artifactRepository.startsWith("http")) {
                    coordToResult.put(res.coordinate, res);
                    return;
                }

                // some packages lack information about the artifact repository in the .m2
                // folder, caused byold Maven tools and FileLockExceptions in multi-threaded
                // executions. This can be recovered through deletion and retry.
                File f = res.getLocalPackageFile();
                if (i == 0 && f.exists() && f.isFile()) {
                    LOG.info("Deleting local package to enforce repository discovery on re-download: {}",
                            res.coordinate);
                    f.delete();
                } else {
                    // deletion "on-the-fly" does not work on windows
                    if (IS_OS_WINDOWS) {
                        LOG.error("Cannot find artifactRepository for {}.", res.coordinate);
                    } else {
                        throw new NoArtifactRepositoryException(res.coordinate);
                    }
                }
            });
        });

        return new HashSet<>(coordToResult.values());
    }

    private static Set<ResolutionResult> resolvePom(File f, String artifactRepository) {
        var res = new HashSet<String[]>();
        try {
            MavenResolvedArtifactImpl.artifactRepositories = res;
            Maven.configureResolver() //
                    .withClassPathResolution(false) //
                    .withRemoteRepo(getRepo(artifactRepository)) //
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
        Maven.configureResolver() //
                .withClassPathResolution(false) //
                .withRemoteRepo(getRepo(artifact.artifactRepository)) //
                .resolve(artifact.coordinate.replace("?", "pom")) //
                .withoutTransitivity() //
                .asResolvedArtifact();
    }

    private static MavenRemoteRepository getRepo(String url) {
        return MavenRemoteRepositories //
                .createRemoteRepository(getRepoName(url), url, "default") //
                .setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_WARN) //
                .setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER);
    }

    private static String getRepoName(String url) {
        var simplifiedUrl = url.replaceAll("[^a-zA-Z0-9-]+", "");
        return format("%s-%s", Resolver.class.getName(), simplifiedUrl);
    }
}