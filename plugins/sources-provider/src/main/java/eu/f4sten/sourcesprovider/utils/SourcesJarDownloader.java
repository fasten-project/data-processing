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

import com.google.inject.Inject;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class SourcesJarDownloader {
    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static final Logger LOG = LoggerFactory.getLogger(SourcesJarDownloader.class);
    private final IoUtils io;

    @Inject
    public SourcesJarDownloader(IoUtils io) {
        this.io = io;
    }

    public Path downloadSourcesJar(MavenId mavenId, String sourcesUrl) {
        try {
            var toPath = createSourcesPath(mavenId);
            LOG.info("Downloading sources Jar from: " + sourcesUrl);
            var request = HttpRequest.newBuilder().GET().uri(URI.create(sourcesUrl)).build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(toPath));
            if(response.statusCode() == 200) {
                return toPath;
            } else {
                throw new IllegalStateException("Could not download sources Jar from: " + sourcesUrl);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Path createSourcesPath(MavenId mavenId) {
        var baseDir = io.getBaseFolder();
        var newPath = Path.of(baseDir.toString(), "sources",
                Constants.mvnForge,
                mavenId.groupId.substring(0, 1),
                mavenId.groupId,
                mavenId.artifactId,
                mavenId.version).toFile();
        if(!newPath.exists()) {
            if(!newPath.mkdirs()) {
                throw new IllegalStateException("Failed to create new sources dir: " + newPath);
            }
        }
        return Path.of(newPath.toString(), mavenId.artifactId + "-" + mavenId.version + "-sources.jar");
    }
}

