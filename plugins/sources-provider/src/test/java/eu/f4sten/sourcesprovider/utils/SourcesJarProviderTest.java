/*
 * Copyright 2022 Software Improvement Group
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
package eu.f4sten.sourcesprovider.utils;

import static dev.c0ps.commons.ResourceUtils.getTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.c0ps.io.IoUtils;
import eu.f4sten.pomanalyzer.data.MavenId;

class SourcesJarProviderTest {

    private SourcesJarProvider provider;
    private File baseDir = null;
    private File tempDir = null;

    private final URL testUrl = new URL("https://repo.maven.apache.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17-sources.jar");

    SourcesJarProviderTest() throws MalformedURLException {}

    @BeforeEach
    void setUp() throws IOException {
        baseDir = Files.createTempDirectory("SourcesDownloaderTest-Base").toFile();
        tempDir = Files.createTempDirectory("SourcesDownloaderTest-Temp").toFile();
        IoUtils io = mock(IoUtils.class);
        SourcesDownloader sd = mock(SourcesDownloader.class);
        provider = new SourcesJarProvider(io, sd);
        when(io.getBaseFolder()).thenReturn(baseDir);
        when(io.getTempFolder()).thenReturn(tempDir);
        when(sd.getFromUrl(testUrl)).thenReturn(getTestResource("SourcesJarProviderTest/log4j-1.2.17-sources"));
    }

    @Test
    void successfulDownloadSourcesJarTest() {
        var mavenId = new MavenId();
        mavenId.groupId = "log4j";
        mavenId.artifactId = "log4j";
        mavenId.version = "1.2.17";
        var sourcesPath = provider.downloadSourcesJar(mavenId, testUrl);
        assertNotNull(sourcesPath);
        var files = new File(sourcesPath).listFiles();
        var actuals = Arrays.stream(files) //
                .map(f -> f.getName()) //
                .collect(Collectors.toSet());
        var expecteds = Set.of("META-INF", "org");
        assertEquals(expecteds, actuals);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(baseDir);
        FileUtils.deleteDirectory(tempDir);
    }
}