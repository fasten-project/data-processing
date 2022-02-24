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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.jboss.shrinkwrap.resolver.api.InvalidConfigurationFileException;
import org.jboss.shrinkwrap.resolver.api.NoResolvedResultException;
import org.jboss.shrinkwrap.resolver.impl.maven.logging.LogRepositoryListener;
import org.jboss.shrinkwrap.resolver.impl.maven.logging.LogTransferListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.fasten.core.maven.utils.MavenUtilities;
import eu.fasten.core.utils.TestUtils;

public class EffectiveModelBuilderTest {

    @BeforeAll
    public static void disableJulLogging() {
        for (var n : Set.of( //
                LogTransferListener.class.getName(), //
                LogRepositoryListener.class.getName())) {
            Logger.getLogger(n).setLevel(Level.OFF);
        }
    }

    @Test
    public void invalidSyntax() {
        assertThrows(InvalidConfigurationFileException.class, () -> {
            buildEffectiveModel("invalid-syntax.pom");
        });
    }

    @Test
    public void invalidNonExistingDep() {
        assertThrows(NoResolvedResultException.class, () -> {
            buildEffectiveModel("invalid-non-existing-dep.pom");
        });
    }

    @Test
    public void basic() {
        var model = buildEffectiveModel("basic.pom");

        var actual = model.getDependencies().stream() //
                .map(d -> String.format("%s:%s", d.getArtifactId(), d.getVersion())) //
                .collect(Collectors.toSet());
        var expected = deps("commons-lang3:3.12.0");
        assertEquals(expected, actual);
    }

    @Test
    public void inheritedDependency() {
        var model = buildEffectiveModel("inherited-dependency.pom");

        var actual = model.getDependencies().stream() //
                .map(d -> String.format("%s:%s", d.getArtifactId(), d.getVersion())) //
                .collect(Collectors.toSet());
        assertTrue(actual.contains("guava:18.0"));
        assertTrue(actual.contains("commons-lang3:3.0"));
        // ... and others, left out for brevity
    }

    @Test
    public void inheritedVersion() {
        var model = buildEffectiveModel("inherited-version.pom");

        var actual = model.getVersion();
        var expected = "9";
        assertEquals(expected, actual);
    }

    @Test
    public void inheritedProperty() {
        var model = buildEffectiveModel("inherited-property.pom");

        var actual = model.getProperties().getOrDefault("project.build.sourceEncoding", null);
        var expected = "UTF-8";
        assertEquals(expected, actual);
    }

    @Test
    public void propertyReplacement() {
        var model = buildEffectiveModel("property-replacement.pom");

        var actual = model.getDependencies().stream() //
                .map(d -> String.format("%s:%s", d.getArtifactId(), d.getVersion())) //
                .collect(Collectors.toSet());
        var expected = deps("commons-lang3:3.12.0");
        assertEquals(expected, actual);
    }

    private static Model buildEffectiveModel(String pathToPom) {
        var fullPath = Path.of(EffectiveModelBuilderTest.class.getSimpleName(), pathToPom);
        File pom = TestUtils.getTestResource(fullPath.toString());
        // resolve once to make sure all dependencies exist in local repo
        new Resolver().resolveDependenciesFromPom(pom, MavenUtilities.MAVEN_CENTRAL_REPO);
        var sut = new EffectiveModelBuilder();
        return sut.buildEffectiveModel(pom);
    }

    private static Set<String> deps(String... deps) {
        Set<String> res = new HashSet<String>();
        for (String dep : deps) {
            res.add(dep);
        }
        return res;
    }
}