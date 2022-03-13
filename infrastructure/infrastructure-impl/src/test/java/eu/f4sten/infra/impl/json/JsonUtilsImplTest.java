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
package eu.f4sten.infra.impl.json;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.f4sten.infra.json.TRef;

public class JsonUtilsImplTest {

    private ObjectMapper om;
    private JsonUtilsImpl sut;

    @BeforeEach
    public void setup() {
        om = mock(ObjectMapper.class);
        sut = new JsonUtilsImpl(om);
    }

    @Test
    public void delegatesToJson() throws JsonProcessingException {
        var in = "in";
        var out = "out";
        when(om.writeValueAsString(eq(in))).thenReturn(out);
        var actual = sut.toJson(in);
        assertSame(out, actual);
    }

    @Test
    public void delegatesFromJsonClass() throws JsonProcessingException {
        var in = "in";
        var out = new Object();
        when(om.readValue(eq(in), eq(Object.class))).thenReturn(out);
        var actual = sut.fromJson(in, Object.class);
        assertSame(out, actual);
    }

    @Test
    public void delegatesFromJsonTRef() throws JsonProcessingException {
        var in = "in";
        var out = new Object();
        var tref = new TRef<Object>() {};
        when(om.readValue(eq(in), eq(tref))).thenReturn(out);
        var actual = sut.fromJson(in, tref);
        assertSame(out, actual);
    }
}