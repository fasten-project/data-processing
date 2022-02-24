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

import static eu.fasten.core.maven.utils.MavenUtilities.MAVEN_CENTRAL_REPO;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.NoResolvedResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.f4sten.pomanalyzer.data.ResolutionResult;
import eu.f4sten.test.TestLoggerUtils;
import eu.fasten.core.utils.TestUtils;

// The artifact source resolution breaks caching mechanisms by deleting packages from the
// local .m2 folder. This exact functionality is tested here, so the test suite will download
// dependencies over-and-over again on every build. Enable this test only for local tests.
@Disabled
public class ResolverTest {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";

    @Test
    public void reminderToReenableDisabledAnnotationOnClass() {
        fail("Suite is expensive and should only be run locally. Re-enable @Disabled annotation.");
    }

    private Resolver sut;

    @BeforeEach
    public void setup() {
        sut = new Resolver();
        TestLoggerUtils.clearLog();
    }

    @Test
    public void pomResolution() {
        var jar = new ResolutionResult("io.vertx:vertx-core:jar:4.2.4", MAVEN_CENTRAL);

        for (var packaging : new String[] { "?", "pom", "jar" }) {
            var gapv = format("io.vertx:vertx-core:%s:4.2.4", packaging);
            var r = new ResolutionResult(gapv, MAVEN_CENTRAL);

            FileUtils.deleteQuietly(r.localPomFile);
            FileUtils.deleteQuietly(r.getLocalPackageFile());
            assertFalse(r.localPomFile.exists());
            assertFalse(r.getLocalPackageFile().exists());

            sut.resolveIfNotExisting(r);
            assertTrue(r.localPomFile.exists(), r.localPomFile.toString());
            if (!"?".equals(packaging)) {
                assertTrue(jar.getLocalPackageFile().exists());
            }
        }
    }

    @Test
    public void pomResolutionFailureHandling() {
        var r = new ResolutionResult("g:a:?:1", "http://non.existing.repo/m2/");
        assertThrows(NoResolvedResultException.class, () -> sut.resolveIfNotExisting(r));
    }

    @Test
    public void pomResolutionLogging_NeedResolution() {
        var r = new ResolutionResult("io.vertx:vertx-core:jar:4.2.4", MAVEN_CENTRAL);
        FileUtils.deleteQuietly(r.localPomFile);
        FileUtils.deleteQuietly(r.getLocalPackageFile());
        sut.resolveIfNotExisting(r);
        TestLoggerUtils.assertLogsContain(Resolver.class,
                "INFO Resolving/downloading POM file that does not exist in .m2 folder ...");
    }

    @Test
    public void pomResolutionLogging_PreExists() {
        var r = new ResolutionResult("io.vertx:vertx-core:jar:4.2.4", MAVEN_CENTRAL);
        sut.resolveIfNotExisting(r);
        TestLoggerUtils.clearLog();
        sut.resolveIfNotExisting(r);
        TestLoggerUtils.assertLogsContain(Resolver.class,
                "INFO Found artifact in .m2 folder: io.vertx:vertx-core:jar:4.2.4 (%s)", MAVEN_CENTRAL);
    }

    @Test
    public void resolveDirectDependencies() {
        var actual = resolveTestPom("basic.pom");
        var expected = new HashSet<ResolutionResult>();
        expected.add(JSR305);
        expected.add(COMMONS_LANG3);
        expected.add(REMLA);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveDirectDependenciesWithTraiilingSlash() {
        var actual = resolveTestPom("basic-with-trailing-repo-slashes.pom");
        var expected = new HashSet<ResolutionResult>();
        expected.add(JSR305);
        expected.add(COMMONS_LANG3);
        expected.add(REMLA);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveTransitiveDependencies() {
        var actual = resolveTestPom("transitive.pom");
        var expected = new HashSet<ResolutionResult>();
        expected.add(COMMONS_TEXT);
        expected.add(COMMONS_LANG3);

        assertEquals(expected, actual);
    }

    @Test
    public void ignoresTestAndImportScopes() {
        var actual = resolveTestPom("scopes.pom");
        assertTrue(actual.contains(JSR305), "JSR305"); // default (none)
        assertTrue(actual.contains(SLF4J), "SLF4J"); // compile
        assertTrue(actual.contains(COMMONS_TEXT), "COMMONS_TEXT"); // runtime
        assertTrue(actual.contains(COMMONS_LANG3), "COMMONS_LANG3"); // provided
        assertTrue(actual.contains(OKIO), "OKIO"); // system

        assertFalse(actual.contains(OSS_PARENT), "OSS_PARENT"); // import
        assertFalse(actual.contains(JUNIT), "JUNIT"); // test

        // direct and transitive dependencies
        // TODO not clear where the Kotlin reference comes from?!
        assertEquals(5 + 1, actual.size());
    }

    @Test
    public void noDependencies() {
        var actual = resolveTestPom("no-dependencies.pom");
        var expected = new HashSet<ResolutionResult>();
        assertEquals(expected, actual);
    }

    @Test
    public void unresolvableDependencies() {
        assertThrows(NoResolvedResultException.class, () -> {
            resolveTestPom("unresolvable.pom");
        });
    }

    private static final ResolutionResult JSR305 = new ResolutionResult(//
            "com.google.code.findbugs:jsr305:jar:3.0.2", //
            MAVEN_CENTRAL);

    private static final ResolutionResult SLF4J = new ResolutionResult(//
            "org.slf4j:slf4j-api:jar:1.7.32", //
            MAVEN_CENTRAL);

    private static final ResolutionResult COMMONS_LANG3 = new ResolutionResult(//
            "org.apache.commons:commons-lang3:jar:3.9", //
            MAVEN_CENTRAL);

    private static final ResolutionResult COMMONS_TEXT = new ResolutionResult(//
            "org.apache.commons:commons-text:jar:1.8", //
            MAVEN_CENTRAL);

    private static final ResolutionResult OKIO = new ResolutionResult(//
            "com.squareup.okio:okio:jar:3.0.0", //
            MAVEN_CENTRAL);

    private static final ResolutionResult OSS_PARENT = new ResolutionResult(//
            "org.sonatype.oss:oss-parent:jar:2.6.3", //
            MAVEN_CENTRAL);

    private static final ResolutionResult JUNIT = new ResolutionResult(//
            "org.junit.jupiter:junit-jupiter-api:jar:5.8.2", //
            MAVEN_CENTRAL);

    private static final ResolutionResult REMLA = new ResolutionResult(//
            "remla:mylib:jar:0.0.5", //
            "https://gitlab.com/api/v4/projects/26117144/packages/maven/");

    private Set<ResolutionResult> resolveTestPom(String pathToPom) {
        var fullPath = Path.of(ResolverTest.class.getSimpleName(), pathToPom);
        File pom = TestUtils.getTestResource(fullPath.toString());
        return sut.resolveDependenciesFromPom(pom, MAVEN_CENTRAL_REPO);
    }
}