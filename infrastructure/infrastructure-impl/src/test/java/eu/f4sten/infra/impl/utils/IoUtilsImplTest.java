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
package eu.f4sten.infra.impl.utils;

import static eu.f4sten.test.TestLoggerUtils.assertLogsContain;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.test.TestLoggerUtils;

public class IoUtilsImplTest {

    private static final String SOME_IN = "some_in";
    private static final String SOME_OUT = "some_out";

    @TempDir
    private File dir;
    private JsonUtils jsonUtils;

    private IoUtilsImpl sut;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() {
        jsonUtils = mock(JsonUtils.class);
        when(jsonUtils.toJson(eq(SOME_IN))).thenReturn(SOME_OUT);
        when(jsonUtils.fromJson(eq(SOME_OUT), eq(String.class))).thenReturn(SOME_IN);
        when(jsonUtils.fromJson(eq(SOME_OUT), any(TRef.class))).thenReturn(SOME_IN);

        sut = new IoUtilsImpl(dir, jsonUtils);
    }

    @Test
    public void baseFolder() throws IOException {
        assertEquals(dir, sut.getBaseFolder());
    }

    @Test
    public void tempFolder() throws IOException {
        var expected = new File(System.getProperty("java.io.tmpdir"));
        assertEquals(expected, sut.getTempFolder());
    }

    @Test
    public void jsonIsWrittenCorrectly() throws IOException {
        var f = new File(dir, "abc.json");

        assertFalse(f.exists());
        sut.writeToFile(SOME_IN, f);
        assertTrue(f.exists());

        var content = FileUtils.readFileToString(f, UTF_8);
        assertEquals(SOME_OUT, content);
    }

    @Test
    public void jsonIsWrittenCorrectlyToNonExistingSubfolder() throws IOException {
        var f = Paths.get(dir.getAbsolutePath(), "sub", "abc.json").toFile();

        assertFalse(f.exists());
        sut.writeToFile(SOME_IN, f);
        assertTrue(f.exists());

        var content = FileUtils.readFileToString(f, UTF_8);
        assertEquals(SOME_OUT, content);
    }

    @Test
    public void jsonWritingFailsForNonAllowedLocation() throws IOException {
        var f = new File(getInaccessibleRoot() + "foo.bar");
        var e = assertThrows(RuntimeException.class, () -> {
            sut.writeToFile(SOME_IN, f);
        });
        assertNotNull(e.getCause());
        var cause = e.getCause();
        assertEquals(FileNotFoundException.class, cause.getClass());
        assertTrue(cause.getMessage().startsWith(f.getAbsolutePath()));
    }

    @Test
    public void jsonOverridesExistingFile() throws IOException {
        TestLoggerUtils.clearLog();

        var f = new File(dir, "abc.json");

        FileUtils.write(f, "previous value", UTF_8);
        sut.writeToFile(SOME_IN, f);

        var content = FileUtils.readFileToString(f, UTF_8);
        assertEquals(SOME_OUT, content);

        assertLogsContain(IoUtilsImpl.class, "INFO Overriding existing file: %s", f.getAbsoluteFile());
    }

    @Test
    public void jsonIsReadCorrectlyWithClass() throws IOException {
        var f = new File(dir, "abc.json");
        FileUtils.write(f, SOME_OUT, UTF_8);

        var actual = sut.readFromFile(f, String.class);
        assertEquals(SOME_IN, actual);
    }

    @Test
    public void jsonIsReadCorrectlyWithTRef() throws IOException {
        var f = new File(dir, "abc.json");
        FileUtils.write(f, SOME_OUT, UTF_8);

        var actual = sut.readFromFile(f, new TRef<String>() {});
        assertEquals(SOME_IN, actual);
    }

    @Test
    public void jsonReadNonExistingFileWithClass() throws IOException {
        var f = new File(dir, "abc.json");
        var e = assertThrows(RuntimeException.class, () -> {
            sut.readFromFile(f, String.class);
        });
        assertNotNull(e.getCause());
        var cause = e.getCause();
        assertEquals(FileNotFoundException.class, cause.getClass());
        assertTrue(cause.getMessage().startsWith(f.getAbsolutePath()));
    }

    @Test
    public void jsonReadNonExistingFileWithTRef() throws IOException {
        var f = new File(dir, "abc.json");
        var e = assertThrows(RuntimeException.class, () -> {
            sut.readFromFile(f, new TRef<String>() {});
        });
        assertNotNull(e.getCause());
        var cause = e.getCause();
        assertEquals(FileNotFoundException.class, cause.getClass());
        assertTrue(cause.getMessage().startsWith(f.getAbsolutePath()));
    }

    @Test
    public void moveFile() throws IOException {
        var from = new File(dir, "from.json");
        var to = new File(dir, "to.json");
        FileUtils.write(from, SOME_IN, UTF_8);
        sut.move(from, to);
        assertFalse(from.exists());
        assertTrue(to.exists());

        var actual = FileUtils.readFileToString(to, UTF_8);
        assertEquals(SOME_IN, actual);
    }

    @Test
    public void moveFileUnknownFrom() throws IOException {
        var from = new File(dir, "from.json");
        var to = new File(dir, "to.json");
        var e = assertThrows(RuntimeException.class, () -> {
            sut.move(from, to);
        });
        assertNotNull(e.getCause());
        var cause = e.getCause();
        assertEquals(FileNotFoundException.class, cause.getClass());
        var m = String.format("Source '%s' does not exist", from.getAbsolutePath());
        assertEquals(m, cause.getMessage());
    }

    @Test
    public void moveFileToNonExistingSubfolder() throws IOException {
        var from = new File(dir, "from.json");
        var to = Paths.get(dir.getAbsolutePath(), "sub", "to.json").toFile();
        FileUtils.write(from, SOME_IN, UTF_8);
        sut.move(from, to);
        var actual = FileUtils.readFileToString(to, UTF_8);
        assertEquals(SOME_IN, actual);
    }

    @Test
    public void moveFileToInaccessibleRoot() throws IOException {
        var from = new File(dir, "from.json");
        var to = new File(getInaccessibleRoot() + "foo.bar");
        FileUtils.write(from, SOME_IN, UTF_8);
        var e = assertThrows(RuntimeException.class, () -> {
            sut.move(from, to);
        });
        assertNotNull(e.getCause());
        var cause = e.getCause();
        assertEquals(AccessDeniedException.class, cause.getClass());
        assertTrue(cause.getMessage().startsWith(to.getAbsolutePath()));
    }

    @Test
    public void moveFileToExists() throws IOException {
        TestLoggerUtils.clearLog();
        var from = new File(dir, "from.jspn");
        var to = new File(dir, "to.json");
        FileUtils.write(from, SOME_IN, UTF_8);
        FileUtils.write(to, "...", UTF_8);
        sut.move(from, to);
        var actual = FileUtils.readFileToString(to, UTF_8);
        assertEquals(SOME_IN, actual);
        assertLogsContain(IoUtilsImpl.class, "INFO Replacing existing file: %s", to.getAbsoluteFile());
    }

    private static String getInaccessibleRoot() {
        return SystemUtils.IS_OS_WINDOWS ? "C:\\Windows\\" : "/";
    }
}