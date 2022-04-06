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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.javastack.httpd.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.fasten.core.maven.data.Pom;

public class MavenRepositoryUtilsTest {

    private static final String ARTIFACT_REPO = "http://127.0.0.1:1234";
    private static final String SOME_CONTENT = "<some content>";

    @TempDir
    private static File dirHttpd;
    private static HttpServer httpd;

    @TempDir
    private File dirM2;
    private MavenRepositoryUtils sut;

    @BeforeAll
    public static void setupAll() throws IOException {
        httpd = new HttpServer(1234, dirHttpd.getAbsolutePath());
        httpd.start();
    }

    @AfterAll
    public static void teardownAll() {
        httpd.stop();
    }

    @BeforeEach
    public void setup() {
        sut = new MavenRepositoryUtils();
    }

    @AfterEach
    public void teardown() {
        FileUtils.deleteQuietly(dirHttpd);
        dirHttpd.mkdir();
    }

    @Test
    public void localM2FolderExists() {
        File f = MavenRepositoryUtils.getPathOfLocalRepository();
        assertTrue(f.isAbsolute());
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
    }

    @Test
    public void getSourcesUrlExists() {
        var par = minimalPomAnalysisResult();
        webContent(SOME_CONTENT, "g", "a", "1.2.3", "a-1.2.3-sources.jar");
        String expected = ARTIFACT_REPO + "/g/a/1.2.3/a-1.2.3-sources.jar";
        String actual = sut.getSourceUrlIfExisting(par);
        assertEquals(expected, actual);
    }

    @Test
    public void getSourcesUrlExistsWithComplexGroup() {
        var par = minimalPomAnalysisResult();
        par.groupId = "g.h.i";
        webContent(SOME_CONTENT, "g", "h", "i", "a", "1.2.3", "a-1.2.3-sources.jar");
        String expected = ARTIFACT_REPO + "/g/h/i/a/1.2.3/a-1.2.3-sources.jar";
        String actual = sut.getSourceUrlIfExisting(par);
        assertEquals(expected, actual);
    }

    @Test
    public void getSourcesUrlExistsOtherPackaging() {
        var par = minimalPomAnalysisResult();
        par.packagingType = "war";
        webContent(SOME_CONTENT, "g", "a", "1.2.3", "a-1.2.3-sources.war");
        String expected = ARTIFACT_REPO + "/g/a/1.2.3/a-1.2.3-sources.war";
        String actual = sut.getSourceUrlIfExisting(par);
        assertEquals(expected, actual);
    }

    @Test
    public void getSourcesUrlDoesNotExist() {
        var par = minimalPomAnalysisResult();
        assertNull(sut.getSourceUrlIfExisting(par));
    }

    @Test
    public void getSourcesUrlNoGroup() {
        for (var invalid : new String[] { null, "", "?" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var par = minimalPomAnalysisResult();
                par.groupId = invalid;
                sut.getSourceUrlIfExisting(par);
            });
        }
    }

    @Test
    public void getSourcesUrlNoArtifact() {
        for (var invalid : new String[] { null, "", "?" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var par = minimalPomAnalysisResult();
                par.artifactId = invalid;
                sut.getSourceUrlIfExisting(par);
            });
        }
    }

    @Test
    public void getSourcesUrlNoPackaging() {
        for (var invalid : new String[] { null, "", "?" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var par = minimalPomAnalysisResult();
                par.packagingType = invalid;
                sut.getSourceUrlIfExisting(par);
            });
        }
    }

    @Test
    public void getSourcesUrlNoVersion() {
        for (var invalid : new String[] { null, "", "?" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var par = minimalPomAnalysisResult();
                par.version = invalid;
                sut.getSourceUrlIfExisting(par);
            });
        }
    }

    @Test
    public void getSourcesUrlNoArtifactRepo() {
        for (var invalid : new String[] { null, "", "?" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var par = minimalPomAnalysisResult();
                par.artifactRepository = invalid;
                sut.getSourceUrlIfExisting(par);
            });
        }
    }

    @Test
    public void doesNotExist() {
        var res = par("g1.g2:a:pt:1");
        assertFalse(sut.doesExist(res));
    }

    @Test
    public void doesExist() {
        var res = par("g1.g2:a:pt:1");
        webContent("<content>", "g1", "g2", "a", "1", "a-1.pt");
        assertTrue(sut.doesExist(res));
    }

    @Test
    public void getReleaseDate() {
        var par = minimalPomAnalysisResult();
        webContent(SOME_CONTENT, "g", "a", "1.2.3", "a-1.2.3.jar");
        var actual = sut.getReleaseDate(par);
        var expected = new Date().getTime();
        var diff = expected - actual;
        assertTrue(diff < 10 * 1000, String.format("difference must be <10s, was %dms", diff));
    }

    @Test
    public void getReleaseDateNonExisting() {
        var par = minimalPomAnalysisResult();
        var actual = sut.getReleaseDate(par);
        var expected = -1;
        assertEquals(expected, actual);
    }

    private static Pom minimalPomAnalysisResult() {
        var par = new Pom();
        par.artifactRepository = ARTIFACT_REPO;
        par.groupId = "g";
        par.artifactId = "a";
        par.packagingType = "jar";
        par.version = "1.2.3";
        return par;
    }

    private void webContent(String content, String... path) {
        try {
            File f = Path.of(dirHttpd.getAbsolutePath(), path).toFile();
            writeStringToFile(f, content, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pom par(String gapt) {
        String[] parts = gapt.split(":");
        var par = new Pom();
        par.groupId = parts[0];
        par.artifactId = parts[1];
        par.packagingType = parts[2];
        par.version = parts[3];
        par.artifactRepository = ARTIFACT_REPO;
        return par;
    }
}