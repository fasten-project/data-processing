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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.c0ps.franz.Lane;
import dev.c0ps.maveneasyindex.Artifact;
import eu.f4sten.pomanalyzer.data.ResolutionResult;

public class ProgressTrackerTest {

    private static final ResolutionResult SOME_RESULT = new ResolutionResult("g:a:p:v", "http://repo.org/");

    private DatabaseUtils db;
    private ProgressTracker sut;

    @BeforeEach
    public void setup() {
        db = mockDatabase();
        sut = new ProgressTracker(db);
    }

    private static DatabaseUtils mockDatabase() {
        var db = mock(DatabaseUtils.class);
        var counts = new HashMap<Object, Integer>();
        doAnswer(inv -> {
            var key = inv.getArgument(0);
            counts.remove(key);
            return null;
        }).when(db).pruneRetries(anyString());
        doAnswer(inv -> {
            var key = inv.getArgument(0);
            if (counts.containsKey(key)) {
                counts.put(key, counts.get(key) + 1);
            } else {
                counts.put(key, 1);
            }
            return null;
        }).when(db).registerRetry(anyString());
        when(db.getRetryCount(anyString())).thenAnswer(inv -> {
            var key = inv.getArgument(0);
            return counts.get(key);
        });
        return db;
    }

    @Test
    public void getOriginalDefault() {
        assertNull(sut.getCurrentOriginal());
    }

    @Test
    public void originalCanBeSet() {
        var id = mock(Artifact.class);
        sut.startNextOriginal(id);
        assertEquals(id, sut.getCurrentOriginal());
    }

    @Test
    public void crashesPruneCountInDb() {
        sut.executionCrash(SOME_RESULT, Lane.NORMAL);
        verify(db).pruneRetries("g:a:p:v-NORMAL");
    }

    @Test
    public void crashesMarkProgressInMem() {
        assertFalse(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
        sut.executionCrash(SOME_RESULT, Lane.NORMAL);
        assertTrue(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
    }

    @Test
    public void threeRetries() {
        assertFalse(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
        sut.registerRetry(SOME_RESULT, Lane.NORMAL); // 1
        assertFalse(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
        sut.registerRetry(SOME_RESULT, Lane.NORMAL); // 2
        assertFalse(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
        sut.registerRetry(SOME_RESULT, Lane.NORMAL); // 3
        assertFalse(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
        sut.registerRetry(SOME_RESULT, Lane.NORMAL); // 4
        assertTrue(sut.shouldSkip(SOME_RESULT, Lane.NORMAL));
    }

    // TODO add more tests
}