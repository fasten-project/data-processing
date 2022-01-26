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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.f4sten.pomanalyzer.data.MavenId;

public class FileReaderTest {

    private FileReader sut;

    @BeforeEach
    public void setup() {
        sut = new FileReader();
    }

    @Test
    public void integrationTest() {
        var ids = sut.readIndexFile(getExampleIndex());
        assertEquals(330, ids.size());

        assertTrue(ids.contains(id("com.github.fcofdez:alcaudon_2.12:0.0.36")));
        assertTrue(ids.contains(id("fi.testee:testeefi-cucumber:0.5.3")));
        assertTrue(ids.contains(id("com.coreoz:plume-archetypes-parent:1.0.0")));
        assertTrue(ids.contains(id("de.carne:java-default:4")));
        // ... and others
    }

    private MavenId id(String s) {
        var parts = s.split(":");
        var id = new MavenId();
        id.groupId = parts[0];
        id.artifactId = parts[1];
        id.version = parts[2];
        return id;
    }

    private static File getExampleIndex() {
        return Paths.get("src", "test", "resources", "some-index.gz").toFile();
    }
}