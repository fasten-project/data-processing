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

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.f4sten.infra.json.ObjectMapperBuilder;
import eu.f4sten.pomanalyzer.json.CoreJacksonModule;
import eu.fasten.core.data.Constants;
import eu.fasten.core.maven.data.Dependency;

public class PomAnalysisResultTest {

    @Test
    public void defaults() {
        var sut = new PomAnalysisResult();
        assertNull(sut.artifactId);
        assertNull(sut.artifactRepository);
        assertNull(sut.commitTag);
        assertNotNull(sut.dependencies);
        assertNotNull(sut.dependencyManagement);
        assertEquals(Constants.mvnForge, sut.forge);
        assertNull(sut.groupId);
        assertNull(sut.packagingType);
        assertNull(sut.parentCoordinate);
        assertNull(sut.projectName);
        assertEquals(-1L, sut.releaseDate);
        assertNull(sut.repoUrl);
        assertNull(sut.sourcesUrl);
        assertNull(sut.version);
    }

    @Test
    public void equalityDefault() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityNonDefault() {
        var a = somePomAnalysisResult();
        var b = somePomAnalysisResult();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffArtifact() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.artifactId = "a";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffArtifactRepo() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.artifactRepository = "b";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffCommitTag() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.commitTag = "c";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffDep() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.dependencies.add(someDependency("d"));

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffDepMgmt() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.dependencyManagement.add(someDependency("e"));

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffForge() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.forge = "f";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffGroup() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.groupId = "g";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffPackaging() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.packagingType = "h";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffParent() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.parentCoordinate = "i";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffName() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.projectName = "j";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffReleaseDate() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.releaseDate = 123;

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffRepoUrl() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.repoUrl = "k";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffSourcesUrl() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.sourcesUrl = "j";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDiffVersion() {
        var a = new PomAnalysisResult();
        var b = new PomAnalysisResult();
        b.version = "k";

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hasToString() {
        var sut = new PomAnalysisResult();
        var actual = sut.toString();

        assertTrue(actual.contains(PomAnalysisResult.class.getSimpleName()));
        assertTrue(actual.contains("\n"));
        assertTrue(actual.split("\n")[0].contains("@"));
        assertTrue(actual.contains("artifactId"));
    }

    @Test
    public void canBeCloned() {
        var a = somePomAnalysisResult();
        var b = a.clone();
        assertEquals(a, b);
        assertNotSame(a, b);

        assertEquals(a.dependencies, b.dependencies);
        assertNotSame(a.dependencies, b.dependencies);

        assertEquals(a.dependencyManagement, b.dependencyManagement);
        assertNotSame(a.dependencyManagement, b.dependencyManagement);
    }

    @Test
    public void dependencyOrderIsPreserved() throws JsonProcessingException {
        var sut = new PomAnalysisResult();
        range(0, 100).forEach(num -> {
            sut.dependencies.add(new Dependency("g", "a", String.valueOf(num)));
        });

        var check = 0;
        for (var d : sut.dependencies) {
            var v = assertSingleVersionConstraint(d);
            assertEquals(check++, Integer.valueOf(v));
        }

        check = 0;
        for (var d : jsonRoundtrip(sut).dependencies) {
            var v = assertSingleVersionConstraint(d);
            assertEquals(check++, Integer.valueOf(v));
        }
    }

    @Test
    public void toCoordinate() {
        var actual = somePomAnalysisResult().toCoordinate();
        var expected = "g:a:h:n";
        assertEquals(expected, actual);
    }

    private static PomAnalysisResult somePomAnalysisResult() {
        PomAnalysisResult r = new PomAnalysisResult();
        r.artifactId = "a";
        r.artifactRepository = "b";
        r.commitTag = "c";
        r.dependencies.add(someDependency("d"));
        r.dependencyManagement.add(someDependency("e"));
        r.forge = "f";
        r.groupId = "g";
        r.packagingType = "h";
        r.parentCoordinate = "i";
        r.projectName = "j";
        r.releaseDate = 123;
        r.repoUrl = "k";
        r.sourcesUrl = "m";
        r.version = "n";
        return r;
    }

    private static Dependency someDependency(String name) {
        return new Dependency("dep", name, "0.0.1");
    }

    private static PomAnalysisResult jsonRoundtrip(PomAnalysisResult sut)
            throws JsonProcessingException, JsonMappingException {
        var om = new ObjectMapperBuilder() {
            @Override
            protected ObjectMapper addMapperOptions(ObjectMapper om) {
                om.registerModule(new CoreJacksonModule());
                return om;
            }
        }.build();
        var json = om.writeValueAsString(sut);
        // make sure custom serializers are registerd
        assertTrue(json.contains("\"versionConstraints\":[\"0\"]"));
        var sut2 = om.readValue(json, PomAnalysisResult.class);
        assertEquals(sut, sut2);
        return sut2;
    }

    private static String assertSingleVersionConstraint(Dependency d) {
        var cs = d.versionConstraints;
        assertEquals(1, cs.size());
        return cs.iterator().next().spec;
    }
}