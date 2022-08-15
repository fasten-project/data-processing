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
package eu.f4sten.sourcesprovider.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SourcePayloadTest {

    @Test
    public void defaults1() {
        var sut = new SourcePayload();
        assertNull(sut.getForge());
        assertNull(sut.getProduct());
        assertNull(sut.getVersion());
        assertNull(sut.getSourcePath());
    }

    @Test
    public void defaults2() {
        var sut = new SourcePayload("f", "p:q", "1.2.3", "/a/b/c");
        assertEquals("f", sut.getForge());
        assertEquals("p:q", sut.getProduct());
        assertEquals("1.2.3", sut.getVersion());
        assertEquals("/a/b/c", sut.getSourcePath());
    }

    @Test
    public void setForge() {
        var sut = new SourcePayload();
        sut.setForge("f");
        assertEquals("f", sut.getForge());
    }

    @Test
    public void setProduct() {
        var sut = new SourcePayload();
        sut.setProduct("p:q");
        assertEquals("p:q", sut.getProduct());
    }

    @Test
    public void setVersion() {
        var sut = new SourcePayload();
        sut.setVersion("1.2.3");
        assertEquals("1.2.3", sut.getVersion());
    }

    @Test
    public void setSourcePath() {
        var sut = new SourcePayload();
        sut.setSourcePath("/a/b/c");
        assertEquals("/a/b/c", sut.getSourcePath());
    }

    @Test
    public void equalityDefault() {
        var a = new SourcePayload();
        var b = new SourcePayload();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityNonDefault() {
        var a = new SourcePayload("f", "p:q", "1.2.3", "/a/b/c");
        var b = new SourcePayload("f", "p:q", "1.2.3", "/a/b/c");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringIsImplemented() {
        var actual = new SourcePayload().toString();
        assertTrue(actual.contains(SourcePayload.class.getSimpleName()));
        assertTrue(actual.contains("\n"));
        assertTrue(actual.contains("forge"));
        assertTrue(actual.contains("@"));
    }
}