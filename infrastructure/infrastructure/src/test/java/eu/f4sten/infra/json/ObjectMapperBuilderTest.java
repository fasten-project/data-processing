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
package eu.f4sten.infra.json;

import static com.fasterxml.jackson.databind.SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper.Builder;

public class ObjectMapperBuilderTest {

    private static final SerializationFeature SOME_FEATURE = USE_EQUALITY_FOR_OBJECT_ID;

    private ObjectMapper sut;

    @Test
    public void defaultConfig() {
        sut = new ObjectMapperBuilder().build();
        assertFalse(sut.isEnabled(SOME_FEATURE));
    }

    @Test
    public void canAddBuilderOptions() {
        sut = new ObjectMapperBuilder() {
            @Override
            protected Builder addBuilderOptions(Builder b) {
                return b.enable(SOME_FEATURE);
            }
        }.build();
        assertTrue(sut.isEnabled(SOME_FEATURE));
    }

    @Test
    public void canAddMapperOptions() {
        sut = new ObjectMapperBuilder() {
            protected ObjectMapper addMapperOptions(ObjectMapper om) {
                return om.enable(SOME_FEATURE);
            }
        }.build();
        assertTrue(sut.isEnabled(SOME_FEATURE));
    }
}