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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.test.TestLoggerUtils;
import eu.fasten.core.json.ObjectMapperBuilder;

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
        var om = new ObjectMapperBuilder().build();
        sut = new IoUtilsImpl(dir, jsonUtils, om);
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
        var from = new File(dir, "from.json");
        var to = new File(dir, "to.json");
        FileUtils.write(from, SOME_IN, UTF_8);
        FileUtils.write(to, "...", UTF_8);
        sut.move(from, to);
        var actual = FileUtils.readFileToString(to, UTF_8);
        assertEquals(SOME_IN, actual);
        assertLogsContain(IoUtilsImpl.class, "INFO Replacing existing file: %s", to.getAbsoluteFile());
    }

    @Test
    public void zipRoundtrip_type() {
        var expected = "e";
        File f = new File(dir, "abc.zip");
        sut.writeToZip(expected, f);
        var actual = sut.readFromZip(f, String.class);
        assertEquals(expected, actual);
    }

    @Test
    public void zipRoundtrip_typeRef() {
        var expecteds = Set.of("a", "b", "c");
        File f = new File(dir, "abc.zip");
        sut.writeToZip(expecteds, f);
        var actuals = sut.readFromZip(f, new TRef<Set<String>>() {});
        assertEquals(expecteds, actuals);
    }

    @Test
    public void zipReadDirectory_type() {
        File f = new File(dir, "folder");
        f.mkdir();
        var e = assertThrows(RuntimeException.class, () -> {
            sut.readFromZip(f, String.class);
        });
        assertTrue(e.getCause() instanceof FileNotFoundException);
        assertTrue(e.getMessage().contains(f.getAbsolutePath()));
    }

    @Test
    public void zipReadDirectory_typeRef() {
        File f = new File(dir, "folder");
        f.mkdir();
        var e = assertThrows(RuntimeException.class, () -> {
            sut.readFromZip(f, new TRef<Set<String>>() {});
        });
        assertTrue(e.getCause() instanceof FileNotFoundException);
        assertTrue(e.getMessage().contains(f.getAbsolutePath()));
    }

    @Test
    public void readingNonExistingZip() {
        var e = assertThrows(RuntimeException.class, () -> {
            sut.readFromZip(new File("doesNotExist.zip"), String.class);
        });
        assertTrue(e.getCause() instanceof NoSuchFileException);
        assertTrue(e.getMessage().contains("doesNotExist.zip"));
    }

    @Test
    public void readingZipWarnsAboutMissedEntries() {
        TestLoggerUtils.clearLog();
        var f = new File(dir, "abc.zip");

        try (//
                var fos = new FileOutputStream(f); //
                var zos = new ZipOutputStream(fos)) {

            zos.putNextEntry(new ZipEntry("a"));
            zos.write("\"abc\"".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("x"));
            zos.write("\"xyz\"".getBytes());
            zos.closeEntry();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var actual = sut.readFromZip(f, String.class);
        var expected = "abc";
        assertEquals(expected, actual);

        assertLogsContain(IoUtilsImpl.class,
                "WARN Only the first entry of .zip file is read, all other entries will be ignored: %s",
                f.getAbsolutePath());
    }

    @Test
    public void writingZipRewritesContentFileNameZip() {
        var expecteds = Set.of("a", "b", "c");
        File f = new File(dir, "abc.zip");
        sut.writeToZip(expecteds, f);
        assertEntryNames(f, "abc.json");
    }

    @Test
    public void writingZipRewritesContentFileNameOther() {
        var expecteds = Set.of("a", "b", "c");
        File f = new File(dir, "abc.zp");
        sut.writeToZip(expecteds, f);
        assertEntryNames(f, "abc.zp.json");
    }

    private static void assertEntryNames(File f, String... names) {
        var expecteds = Set.of(names);
        var actuals = new HashSet<>();
        try (var zf = new ZipFile(f)) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                var next = entries.nextElement();
                actuals.add(next.getName());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(expecteds, actuals);
    }

    private static String getInaccessibleRoot() {
        return SystemUtils.IS_OS_WINDOWS ? "C:\\Windows\\" : "/";
    }
}