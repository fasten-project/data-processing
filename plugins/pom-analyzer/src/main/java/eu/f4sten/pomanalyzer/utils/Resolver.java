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
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.COMPILE;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.PROVIDED;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.RUNTIME;
import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.SYSTEM;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenResolvedArtifactImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.pomanalyzer.data.ResolutionResult;

public class Resolver {

    private static final Logger LOG = LoggerFactory.getLogger(Resolver.class);

    // Attention: Be aware that the test suite for this class is disabled by default
    // to avoid unnecessary downloads on every build. Make sure to re-enable the
    // tests and run them locally for every change in this class.

    private Predicate<String> funShouldSkip;

    public Resolver() {
        // by default, nothing gets skipped
        this(s -> false);
    }

    /**
     * @param funShouldSkip Registers a Predicate that can prevent the processing of
     *                      a coordinate in the form gid:aid:packaging:version
     */
    public Resolver(Predicate<String> funShouldSkip) {
        this.funShouldSkip = funShouldSkip;
    }

    public Set<ResolutionResult> resolveDependenciesFromPom(File pom) {
        var coordToResult = new HashMap<String, ResolutionResult>();

        // two iterations: 0) resolving and (potential) deletion 1) get artifactRepos
        range(0, 2).forEach(i -> {
            resolvePom(pom).forEach(res -> {
                // ignore known dependencies or those that should be skipped (e.g., exist in DB)
                if (coordToResult.containsKey(res.coordinate) || funShouldSkip.test(res.coordinate)) {
                    return;
                }

                // remember identified artifactRepository
                if (res.artifactRepository.startsWith("http")) {
                    coordToResult.put(res.coordinate, res);
                    return;
                }

                // if a package is interesting, but has already been downloaded before (i.e., it
                // exists in the local .m2 folder, it must be deleted and re-downloaded (which
                // automatically happens in the second iteration). Otherwise, it is impossible
                // to infer its source artifact repository.
                if (i == 0) {
                    File f = res.getLocalPackageFile();
                    if (f.exists() && f.isFile()) {
                        f.delete();
                    }
                } else {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        LOG.error("Cannot find artifactRepository for {}.", res.coordinate);
                    } else {
                        String msg = "Cannot find artifactRepository for %s.";
                        throw new IllegalStateException(format(msg, res.coordinate));
                    }
                }
            });
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
                    return new ResolutionResult(a[0], a[1], new File(a[2]));
                }) //
                .collect(Collectors.toSet());
    }
}