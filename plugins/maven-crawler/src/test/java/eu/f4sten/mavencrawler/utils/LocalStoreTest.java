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

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;

public class LocalStoreTest {

    private static final int SOME_IDX = 1234;

    @TempDir
    private File tempDir;

    private IoUtils io;
    private MavenCrawlerArgs args;

    private LocalStore sut;

    @BeforeEach
    public void setup() {
        args = new MavenCrawlerArgs();
        args.firstConsideredIndex = SOME_IDX;
        io = mock(IoUtils.class);
        when(io.getBaseFolder()).thenReturn(tempDir);

        sut = new LocalStore(args, io);
    }

    @Test
    public void nonExistingStoreWillReturnDefault() {
        var actual = sut.getNextIndex();
        var expected = SOME_IDX;
        assertEquals(expected, actual);
    }

    @Test
    public void finishingWillCreateFile() throws IOException {
        sut.finish(234);
        var f = getIndexFile();
        assertTrue(f.exists() && f.isFile());
        var actual = FileUtils.readFileToString(f, UTF_8);
        var expected = "235";
        assertEquals(expected, actual);
    }

    @Test
    public void existingFileDeterminesNextIdx() throws IOException {
        sut.finish(234);
        var actual = sut.getNextIndex();
        var expected = 235;
        assertEquals(expected, actual);
    }

    @Test
    public void interestingIndexCannotBeNegative() throws Exception {
        args.firstConsideredIndex = 0;
        new LocalStore(args, io);
        var out = tapSystemOut(() -> {
            catchSystemExit(() -> {
                args.firstConsideredIndex = -1;
                new LocalStore(args, io);
            });
        });
        assertTrue(out.contains("Insufficient startup arguments"));
        assertTrue(out.contains("A requested argument is invalid"));
        assertTrue(out.contains("(first considered index must be positive number)"));
    }

    private File getIndexFile() {
        return Paths.get(tempDir.getAbsolutePath(), "maven-crawler", "index.txt").toFile();
    }
}