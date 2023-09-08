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
package eu.f4sten.swhinserter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.c0ps.franz.Kafka;
import dev.c0ps.franz.Lane;
import dev.c0ps.io.IoUtils;
import eu.f4sten.sourcesprovider.data.SourcePayload;

public class MainTest {

    private static final File SOMEBASE = new File("BASE");
    private static final String SOMEHASH = "12345";
    private Main sut;
    private DatabaseUtils db;
    private IoUtils io;
    private SwhHashCalculator hash;

    @BeforeEach
    public void setup() {
        var args = new SwhInserterArgs();
        var kafka = mock(Kafka.class);
        db = mock(DatabaseUtils.class);
        io = mock(IoUtils.class);
        when(io.getBaseFolder()).thenReturn(SOMEBASE);

        hash = mock(SwhHashCalculator.class);
        when(hash.calc(any(File.class), anyString())).thenReturn(SOMEHASH);
        sut = new Main(args, kafka, db, io, hash);
    }

    @Test
    public void consume() {

        registerPackageVersion("prod:art", "1.2.3", 123);
        registerPaths(123, "a/b/c.txt");

        var p = new SourcePayload("forge", "prod:art", "1.2.3", "a/b/c.txt");
        sut.consume(p, Lane.NORMAL);

        var captor = ArgumentCaptor.forClass(File.class);
        verify(hash).calc(captor.capture(), anyString());

        var expected = new File("BASE/sources/forge/p/prod/art/1.2.3");
        var actual = captor.getValue();
        assertEquals(expected, actual);

        verify(db).addFileHash(eq(123L), eq("a/b/c.txt"), eq(SOMEHASH));
    }

    private void registerPackageVersion(String pkg, String v, long id) {
        when(db.getPkgVersionID(eq(pkg), eq(v))).thenReturn(id);
    }

    private void registerPaths(long id, String... paths) {
        when(db.getFilePaths4PkgVersion(eq(id))).thenReturn(Arrays.asList(paths));
    }
}