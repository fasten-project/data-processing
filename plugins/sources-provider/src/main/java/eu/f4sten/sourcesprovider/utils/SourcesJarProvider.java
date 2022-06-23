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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class SourcesJarProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SourcesJarProvider.class);
    private final IoUtils io;
    private final SourcesDownloader sd;

    @Inject
    public SourcesJarProvider(IoUtils io, SourcesDownloader sd) {
        this.io = io;
        this.sd = sd;
    }

    public String downloadSourcesJar(MavenId mavenId, URL sourcesUrl) {
        var toPath = createSourcesPath(mavenId);
        try {
            if(new File(toPath).exists()) {
                LOG.info("Sources already present, skipping download: " + toPath);
            } else {
                var tempFile = downloadAndUnpack(sourcesUrl, toPath);
                FileUtils.delete(tempFile);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return toPath;
    }

    public String createSourcesPath(MavenId mavenId) {
        var baseDir = io.getBaseFolder();
        return Path.of(baseDir.toString(), "sources",
                Constants.mvnForge,
                mavenId.groupId.substring(0, 1),
                mavenId.groupId,
                mavenId.artifactId,
                mavenId.version).toString();
    }

    private File downloadAndUnpack(URL sourcesUrl, String toPath) throws IOException, InterruptedException {
        LOG.info("Downloading sources from: " + sourcesUrl + " into: " + toPath);
        var tempFile = sd.getFromUrl(sourcesUrl);
        extractJarFile(tempFile, toPath);
        return tempFile;
    }

    private void extractJarFile(File jarFile, String toPath) throws IOException {
        var jar = new JarFile(jarFile);
        var destPath = Path.of(toPath);
        var entries = jar.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            var entryPath = destPath.resolve(entry.getName());
            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                Files.createDirectories(entryPath.getParent());
                var in = jar.getInputStream(entry);
                var out = new FileOutputStream(entryPath.toFile());
                IOUtils.copy(in, out);
                out.close();
            }
        }
        jar.close();
    }
}

