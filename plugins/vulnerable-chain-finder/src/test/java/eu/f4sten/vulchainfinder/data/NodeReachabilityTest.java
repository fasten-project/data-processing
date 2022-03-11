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

package eu.f4sten.vulchainfinder.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidParameterException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NodeReachabilityTest {

    @Test
    public void defaultValues() {
        var sut = new NodeReachability(123);
        assertEquals(123, sut.targetNode);
        assertNotNull(sut.nextStepTowardsTarget);
        assertTrue(sut.nextStepTowardsTarget.isEmpty());
    }

    @Test
    public void equalityDefaults() {
        var a = new NodeReachability(123);
        var b = new NodeReachability(123);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityWithContent() {
        var a = new NodeReachability(123);
        a.nextStepTowardsTarget.put(1L, 2L);
        var b = new NodeReachability(123);
        b.nextStepTowardsTarget.put(1L, 2L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDifferentTargetNode() {
        var a = new NodeReachability(123);
        var b = new NodeReachability(234);
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityDifferentContent() {
        var a = new NodeReachability(123);
        a.nextStepTowardsTarget.put(1L, 2L);
        var b = new NodeReachability(123);
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringIsImplemented() {
        var actual = new NodeReachability(123).toString();
        assertTrue(actual.contains("\n"));
        assertTrue(actual.contains("@"));
        assertTrue(actual.contains(NodeReachability.class.getSimpleName()));
        assertTrue(actual.contains("targetNode"));
    }

    @Test
    public void isReachingTargetSelf() {
        var sut = new NodeReachability(123);
        assertTrue(sut.isReachingTarget(123));
    }

    @Test
    public void isReachingTargetNonExisting() {
        var sut = new NodeReachability(1);
        assertFalse(sut.isReachingTarget(2));
    }

    @Test
    public void isReachingTargetDirectNeighbor() {
        var sut = new NodeReachability(1);
        sut.nextStepTowardsTarget.put(2L, 1L);
        assertTrue(sut.isReachingTarget(2));
    }

    @Test
    public void isReachingTargetTransitiveNeighbor() {
        var sut = new NodeReachability(1);
        sut.nextStepTowardsTarget.put(2L, 1L);
        sut.nextStepTowardsTarget.put(3L, 2L);
        assertTrue(sut.isReachingTarget(3));
    }

    @Test
    public void getShortestPathSelf() {
        var sut = new NodeReachability(1);
        var actual = sut.getShortestPath(1);
        var expected = List.of(1L);
        assertEquals(expected, actual);
    }

    @Test
    public void failsWhenNoPathExists() {
        assertThrows(InvalidParameterException.class, () -> {
            new NodeReachability(1).getShortestPath(2);
        });
    }

    @Test
    public void getShortestPathDirectNeighbor() {
        var sut = new NodeReachability(1);
        sut.nextStepTowardsTarget.put(2L, 1L);
        var actual = sut.getShortestPath(2);
        var expected = List.of(2L, 1L);
        assertEquals(expected, actual);
    }

    @Test
    public void getShortestPathTransitive() {
        var sut = new NodeReachability(1);
        sut.nextStepTowardsTarget.put(2L, 1L);
        sut.nextStepTowardsTarget.put(3L, 2L);
        var actual = sut.getShortestPath(3);
        var expected = List.of(3L, 2L, 1L);
        assertEquals(expected, actual);
    }
}