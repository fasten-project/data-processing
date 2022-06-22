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

import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.pomanalyzer.data.MavenId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static eu.fasten.core.utils.TestUtils.getTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourcesJarDownloaderTest {

    private SourcesJarDownloader downloader;
    private final File baseDir = Path.of(getTestResource(".").toString(), "SourcesDownloaderTest").toFile();

    @BeforeEach
    void setUp() {
        IoUtils io = mock(IoUtils.class);
        downloader = new SourcesJarDownloader(io);
        when(io.getBaseFolder()).thenReturn(baseDir);
    }

    @Test
    @Disabled
    void successfulDownloadSourcesJarTest() {
        var mavenId = new MavenId();
        mavenId.groupId = "log4j";
        mavenId.artifactId = "log4j";
        mavenId.version = "1.2.17";
        var sourcesUrl = "https://repo.maven.apache.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17-sources.jar";
        var sourcesPath = downloader.downloadSourcesJar(mavenId, sourcesUrl);
        assertEquals(Path.of(baseDir.toString(), "sources", "mvn", "l", "log4j", "log4j", "1.2.17", "log4j-1.2.17-sources.jar"), sourcesPath);
    }

    @Test
    @Disabled
    void failedDownloadSourcesJarTest() {
        var mavenId = new MavenId();
        mavenId.groupId = "log4j";
        mavenId.artifactId = "log4j";
        mavenId.version = "1.2.17";
        var sourcesUrl = "https://repo.maven.apache.org/maven2/log4j-BAD-URL/log4j/1.2.17/log4j-1.2.17-sources.jar";
        assertThrows(IllegalStateException.class, () -> downloader.downloadSourcesJar(mavenId, sourcesUrl));
    }
}