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

import static eu.f4sten.test.AssertArgsUtils.assertArgsValidation;
import static eu.f4sten.test.TestLoggerUtils.clearLog;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.javastack.httpd.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;

public class RemoteUtilsTest {

    private static final String FILE_FORMAT = "index-%d.gz";
    private static final String INDEX_URL = "http://127.0.0.1:1234/" + FILE_FORMAT;
    private static final String SOME_CONTENT = "abcd";

    @TempDir
    private static File dirHttpd;
    private static HttpServer httpd;

    @TempDir
    private static File dirTmp;

    private IoUtils io;
    private MavenCrawlerArgs args;
    private RemoteUtils sut;

    @BeforeAll
    public static void setupAll() throws IOException {
        httpd = new HttpServer(1234, dirHttpd.getAbsolutePath());
        httpd.start();
    }

    @AfterAll
    public static void teardownAll() {
        httpd.stop();
    }

    @BeforeEach
    public void setup() {
        args = new MavenCrawlerArgs();
        args.indexUrl = INDEX_URL;
        io = mock(IoUtils.class);
        when(io.getTempFolder()).thenReturn(dirTmp);
        sut = new RemoteUtils(args, io);
        clearLog();
    }

    @Test
    public void indexUrlCannotBeNull() throws Exception {
        assertArgsValidation("index url cannot be null", () -> {
            args.indexUrl = null;
            sut = new RemoteUtils(args, io);
        });
    }

    @Test
    public void indexUrlCannotBeEmpty() {
        assertArgsValidation("index url cannot be empty", () -> {
            args.indexUrl = "";
            sut = new RemoteUtils(args, io);
        });
    }

    @Test
    public void indexUrlMustContainMarker() {
        assertArgsValidation("index url must contain marker for index number", () -> {
            args.indexUrl = "abc";
            sut = new RemoteUtils(args, io);
        });
    }

    @Test
    public void fileDoesNotExist() {
        assertFalse(sut.exists(1));
    }

    @Test
    public void fileDoesExist() {
        provide(1, SOME_CONTENT);
        assertTrue(sut.exists(1));
    }

    @Test
    public void downloadingNonExistingFails() {
        assertThrows(RuntimeException.class, () -> {
            sut.download(1);
        });
    }

    @Test
    public void downloadingExistingWorks() throws IOException {
        provide(1, SOME_CONTENT);
        var actual = sut.download(1);
        assertTrue(actual.exists());
        assertTrue(actual.getAbsolutePath().startsWith(dirTmp.getAbsolutePath()));
        assertEquals("maven-crawler-1.tmp", actual.getName());
        assertEquals(SOME_CONTENT, readFileToString(actual, UTF_8));
    }

    private void provide(int idx, String content) {
        try {
            var fileName = String.format(FILE_FORMAT, idx);
            var f = new File(dirHttpd, fileName);
            writeStringToFile(f, content, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}