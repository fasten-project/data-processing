/*
 * Copyright 2022 Delft University of Technology
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
package eu.f4sten.pomanalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.Plugin;

public class CleanUpM2Repository implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(CleanUpM2Repository.class);
    private final CleanUpM2RepositoryArgs args;

    @Inject
    public CleanUpM2Repository(CleanUpM2RepositoryArgs args) {
        this.args = args;
    }

    @Override
    public void run() {
        LOG.info("Searching for invalid pom.xml files in: {}", this.args.m2RepositoryPath);

        try {
            Files.walkFileTree(Path.of(this.args.m2RepositoryPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                    if (isPom(path) && !read(path).contains("<project")) {
                        LOG.info("Deleting folder of invalid pom.xml: {}", path);
                        var parent = path.getParent().toFile();
                        FileUtils.deleteQuietly(parent);
                        return FileVisitResult.SKIP_SIBLINGS;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Clean-up is finished");
    }

    private static String read(Path path) throws IOException {
        return FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
    }

    private static boolean isPom(Path path) {
        return path.toString().endsWith(".pom");
    }
}