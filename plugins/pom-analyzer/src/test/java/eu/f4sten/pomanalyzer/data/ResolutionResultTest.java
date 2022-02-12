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
package eu.f4sten.pomanalyzer.data;

import static eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils.getPathOfLocalRepository;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.InvalidParameterException;

import org.junit.jupiter.api.Test;

import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;

public class ResolutionResultTest {

    private static final String SOME_REPO = "http://somewhere/";

    @Test
    public void initStoresValuesAndDerivesPom() {
        var gapv = "g:a:jar:1";
        var sut = new ResolutionResult(gapv, SOME_REPO);

        assertEquals(gapv, sut.coordinate);
        assertEquals(SOME_REPO, sut.artifactRepository);
        assertEquals(getPathOfLocalRepository(), sut.localM2Repository);
        assertEquals(inM2("g", "a", "1", "a-1.pom"), sut.localPomFile);
        assertEquals(inM2("g", "a", "1", "a-1.jar"), sut.getLocalPackageFile());
    }

    @Test
    public void validationOnInitWorks() {
        // ok
        new ResolutionResult("g:a:jar:1", SOME_REPO);
        new ResolutionResult("g:a:?:1", SOME_REPO);
        // fail
        assertInvalidParameter("g:a:1");
        assertInvalidParameter("g:a:jar:1:sources");
        for (var inv : new String[] { "", "?" }) {
            assertInvalidParameter(format("%s:a:jar:1", inv));
            assertInvalidParameter(format("g:%s:jar:1", inv));
            assertInvalidParameter(format("g:a:jar:%s", inv));
        }
        assertInvalidParameter("g:a::1");
    }

    private static void assertInvalidParameter(String gapv) {
        assertThrows(InvalidParameterException.class, () -> new ResolutionResult(gapv, SOME_REPO));
    }

    @Test
    public void initializesCorrectPaths() throws MalformedURLException {
        var gapv = "g.g2:a:?:1";
        var sut = new ResolutionResult(gapv, SOME_REPO);

        assertEquals(inM2("g", "g2", "a", "1", "a-1.pom"), sut.localPomFile);
    }

    @Test
    public void equality() {
        var a = new ResolutionResult("g:a:jar:1", SOME_REPO);
        var b = new ResolutionResult("g:a:jar:1", SOME_REPO);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffGAV() {
        var repository = SOME_REPO;
        var a = new ResolutionResult("g:a:jar:1", repository);
        var b = new ResolutionResult("g:b:jar:1", repository);
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffRepo() {
        var gapv = "g:a:jar:1";
        var a = new ResolutionResult(gapv, SOME_REPO);
        var b = new ResolutionResult(gapv, "http://elsewhere/");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hasToStringImpl() {
        var actual = new ResolutionResult("g:a:jar:1", SOME_REPO).toString();
        assertTrue(actual.contains("\n"));
        var l1 = actual.split("\n")[0];
        assertTrue(l1.contains(ResolutionResult.class.getSimpleName()));
        assertTrue(l1.contains("@"));
        assertTrue(actual.contains("localPomFile"));
    }

    private static File inM2(String... pathToFilename) {
        var m2 = MavenRepositoryUtils.getPathOfLocalRepository().getAbsolutePath();
        return Path.of(m2, pathToFilename).toFile();
    }
}