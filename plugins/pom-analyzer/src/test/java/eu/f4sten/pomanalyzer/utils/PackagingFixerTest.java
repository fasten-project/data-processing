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
package eu.f4sten.pomanalyzer.utils;

import static eu.f4sten.test.TestLoggerUtils.assertLogsContain;
import static eu.f4sten.test.TestLoggerUtils.clearLog;
import static eu.f4sten.test.TestLoggerUtils.getFormattedLogs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import eu.fasten.core.maven.data.PomAnalysisResult;

public class PackagingFixerTest {

    private MavenRepositoryUtils repoUtils;
    private PackagingFixer sut;
    private Set<String> existingPackaging;

    @BeforeEach
    public void setup() {
        clearLog();
        existingPackaging = Set.of("<none>");
        repoUtils = mock(MavenRepositoryUtils.class);
        when(repoUtils.doesExist(any(PomAnalysisResult.class))).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock i) throws Throwable {
                PomAnalysisResult arg = i.getArgument(0);
                return existingPackaging.contains(arg.packagingType);
            }
        });
        sut = new PackagingFixer(repoUtils);
    }

    @Test
    public void returnsExistingWhenItCannotBeSolved() {
        assertResult("xxx", "xxx");
    }

    @Test
    public void packageExists() {
        existingPackaging = Set.of("pom", "jar");
        assertResult("jar", "jar");

        verify(repoUtils, times(1)).doesExist(getPAR("jar"));
    }

    @Test
    public void doesExistIsNotCalledTwice() {
        existingPackaging = Set.of("pom", "war");
        assertResult("jar", "war");

        verify(repoUtils, times(1)).doesExist(getPAR("jar"));
    }

    @Test
    public void triesLowercase() {
        existingPackaging = Set.of("pom", "xxx");
        assertResult("Xxx", "xxx");
        verify(repoUtils, times(1)).doesExist(getPAR("Xxx"));
        verify(repoUtils, times(1)).doesExist(getPAR("xxx"));
    }

    @Test
    public void onlyTriesLowercaseWhenDifferent() {
        existingPackaging = Set.of("pom", "war");
        assertResult("jar", "war");
        verify(repoUtils, times(1)).doesExist(getPAR("jar"));
    }

    @Test
    public void worksForJars() {
        existingPackaging = Set.of("pom", "jar");
        assertResult("xxx", "jar");
        verifyNumberOfAdditionalDoesExistCalls(1);
    }

    @Test
    public void worksForWars() {
        existingPackaging = Set.of("pom", "war");
        assertResult("xxx", "war");
        verifyNumberOfAdditionalDoesExistCalls(2);
    }

    @Test
    public void worksForEars() {
        existingPackaging = Set.of("pom", "ear");
        assertResult("xxx", "ear");
        verifyNumberOfAdditionalDoesExistCalls(3);
    }

    @Test
    public void worksForAars() {
        existingPackaging = Set.of("pom", "aar");
        assertResult("xxx", "aar");
        verifyNumberOfAdditionalDoesExistCalls(4);
    }

    @Test
    public void worksForEjbs() {
        existingPackaging = Set.of("pom", "ejb");
        assertResult("xxx", "ejb");
        verifyNumberOfAdditionalDoesExistCalls(5);
    }

    @Test
    public void isCalledWithClone() {
        existingPackaging = Set.of("pom");
        var orig = getPAR("Xxx");
        sut.checkPackage(orig);
        var captor = ArgumentCaptor.forClass(PomAnalysisResult.class);
        verify(repoUtils, times(8)).doesExist(captor.capture());
        var values = captor.getAllValues();
        assertSame(orig, values.get(0));
        for (int i = 1; i < 7; i++) {
            assertNotSame(orig, values.get(i));
        }
    }

    @Test
    public void noLogWhenExisting() {
        existingPackaging = Set.of("pom", "jar");
        assertResult("jar", "jar");
        List<String> log = getFormattedLogs(PackagingFixer.class);
        assertEquals(0, log.size());
    }

    @Test
    public void logWhenNotFound() {
        assertResult("xxx", "xxx");
        assertLogsContain(PackagingFixer.class, "WARN Neither the coordinate nor its pom can be found.");
    }

    @Test
    public void noLogWhenOnlyPomExisting() {
        existingPackaging = Set.of("pom");
        assertResult("jar", "jar");
        assertLogsContain(PackagingFixer.class, "WARN Pom exists, coordinate not found. No fix available.");
    }

    @Test
    public void logWhenChangeFound() {
        existingPackaging = Set.of("pom", "war");
        assertResult("jar", "war");
        assertLogsContain(PackagingFixer.class, "WARN Coordinate found after fixing packagingType: jar -> war");
    }

    private void assertResult(String inputPackaging, String expectedPackaging) {
        var actualPackaging = sut.checkPackage(getPAR(inputPackaging));
        assertEquals(expectedPackaging, actualPackaging);
    }

    private void verifyNumberOfAdditionalDoesExistCalls(int numAdditional) {
        // 1) orig 2) pom 3) lowercase (not needed) 3) cycle through up to 5 types
        var numCalls = 2 + numAdditional;
        verify(repoUtils, times(numCalls)).doesExist(any(PomAnalysisResult.class));
    }

    private PomAnalysisResult getPAR(String packaging) {
        var par = new PomAnalysisResult();
        par.packagingType = packaging;
        return par;
    }
}