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
package eu.f4sten.mavencrawler.utils;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.io.IoUtils;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;
import jakarta.inject.Inject;

public class LocalStore {

    private final int firstConsideredIndex;
    private final IoUtils io;

    @Inject
    public LocalStore(MavenCrawlerArgs args, IoUtils io) {
        AssertArgs.that(args, a -> a.firstConsideredIndex >= 0, "first considered index must be positive number");
        this.firstConsideredIndex = args.firstConsideredIndex;
        this.io = io;
    }

    public int getNextIndex() {
        try {
            var f = getIndexFile();
            if (f.exists()) {
                var content = readFileToString(f, UTF_8);
                return parseInt(content);
            } else {
                return firstConsideredIndex;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void finish(int idx) {
        try {
            var nextIdx = idx + 1;
            var content = Integer.toString(nextIdx);
            writeStringToFile(getIndexFile(), content, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getIndexFile() {
        return Paths.get(io.getBaseFolder().getPath(), "maven-crawler", "index.txt").toFile();
    }
}