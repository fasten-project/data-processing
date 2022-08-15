package eu.f4sten.swhinserter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SwhHashCalculatorTest {

    @TempDir
    public File root;
    private SwhHashCalculator sut;

    @BeforeEach
    public void setup() {
        sut = new SwhHashCalculator();
    }

    @Test
    public void failsOnTrailingSlash() throws IOException {
        var e = assertThrows(IllegalArgumentException.class, () -> {
            sut.calc(root, "/a/b.txt");
        });
        var msg = e.getMessage();
        assertTrue(msg.contains("path must be relative"));
        assertTrue(msg.contains("/a/b.txt"));
    }

    @Test
    public void happypath() throws IOException {
        var sub = new File(root, "a");

        sub.mkdir();
        var f = new File(sub, "b.txt");
        FileUtils.writeStringToFile(f, "abc", StandardCharsets.UTF_8);

        var actual = sut.calc(root, path("a", "b.txt"));
        // created via: git init && git hash-object -w a/b.txt
        var expected = "f2ba8f84ab5c1bce84a7b441cb1959cfc7093b7f";
        assertEquals(expected, actual);
    }

    @Test
    public void subsubfolder() throws IOException {
        var sub = new File(root, "a");
        var subsub = new File(sub, "b");
        subsub.mkdirs();

        var f = new File(subsub, "c.txt");
        FileUtils.writeStringToFile(f, "abc", StandardCharsets.UTF_8);

        var actual = sut.calc(root, path("a", "b", "c.txt"));
        // created via: git init && git hash-object -w a/b.txt
        var expected = "f2ba8f84ab5c1bce84a7b441cb1959cfc7093b7f";
        assertEquals(expected, actual);
    }

    @Test
    public void pathDoesNotExist() {
        var e = assertThrows(IllegalStateException.class, () -> {
            sut.calc(root, path("a", "b.txt"));
        });

        var msg = e.getMessage();
        assertTrue(msg.contains("File does not exist"));
        assertTrue(msg.contains(root.getAbsolutePath()));
        assertTrue(msg.contains(path("a", "b.txt")));
    }

    private static String path(String first, String... more) {
        return Path.of(first, more).toString();
    }
}