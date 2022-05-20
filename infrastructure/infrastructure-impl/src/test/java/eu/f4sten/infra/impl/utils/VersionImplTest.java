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
package eu.f4sten.infra.impl.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class VersionImplTest {

    private static final String CURRENT_VERSION = "0.0.9-SNAPSHOT";
    private static String srcVersion;

    @BeforeAll
    public static void setupClass() {
        // remember current version from src...
        srcVersion = readSrcVersion();
    }

    @AfterEach
    public void teardown() {
        // ... and restore it after each test
        writeToTarget(srcVersion);
    }

    @Test
    @Disabled
    public void disabled() {
        // prevent organize-import form removing import of annotation
    }

    @Test
    public void getDefault() {
        var actual = new VersionImpl().get();
        var expected = CURRENT_VERSION;
        assertEquals(expected, actual);
    }

    @Test
    public void canBeReplaced() {
        writeToTarget("...");

        var actual = new VersionImpl().get();
        var expected = "...";
        assertEquals(expected, actual);
    }

    @Test
    public void trimsWhitespace() {
        writeToTarget(" \t ... \t ");

        var actual = new VersionImpl().get();
        var expected = "...";
        assertEquals(expected, actual);
    }

    @Test
    public void nonExisting() {
        var f = Paths.get("target", "classes", "version.txt").toFile();
        f.delete();

        var actual = new VersionImpl().get();
        var expected = "n/a";
        assertEquals(expected, actual);
    }

    private static void writeToTarget(String version) {
        try {
            var f = Paths.get("target", "classes", "version.txt").toFile();
            writeStringToFile(f, version, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readSrcVersion() {
        try {
            var f = Paths.get("src", "main", "resources", "version.txt").toFile();
            return FileUtils.readFileToString(f, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}