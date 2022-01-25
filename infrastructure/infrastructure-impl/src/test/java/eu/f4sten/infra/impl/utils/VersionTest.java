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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class VersionTest {

    private File versionFile;

    @AfterEach
    public void teardown() {
        overrideVersion("dev");
    }

    @Test // start with _ to make sure it is executed first
    public void _getDefault() {
        var actual = new VersionImpl().get();
        var expected = "dev";
        assertEquals(expected, actual);
    }

    @Test
    public void canBeReplaced() {
        overrideVersion("...");

        var actual = new VersionImpl().get();
        var expected = "...";
        assertEquals(expected, actual);
    }

    @Test
    public void trimsWhitespace() {
        overrideVersion(" \t ... \t ");

        var actual = new VersionImpl().get();
        var expected = "...";
        assertEquals(expected, actual);
    }

    @Test
    public void nonExisting() {
        getFile().delete();

        var actual = new VersionImpl().get();
        var expected = "n/a";
        assertEquals(expected, actual);
    }

    private void overrideVersion(String version) {
        try {
            writeStringToFile(getFile(), version, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFile() {
        if (versionFile != null) {
            return versionFile;
        }
        var url = getClass().getClassLoader().getResource("version.txt");
        versionFile = new File(url.getFile());
        return versionFile;
    }
}