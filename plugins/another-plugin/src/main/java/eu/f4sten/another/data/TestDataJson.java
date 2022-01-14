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
package eu.f4sten.another.data;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class TestDataJson {

    public static class TestDataDeserializer extends JsonDeserializer<TestData> {

        @Override
        public TestData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            var parts = p.getValueAsString().split(":");
            TestData t = new TestData();
            t.name = parts[0];
            t.age = Integer.valueOf(parts[1]);
            return t;
        }
    }

    public static class TestDataSerializer extends JsonSerializer<TestData> {

        @Override
        public void serialize(TestData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name + ":" + value.age);
        }
    }
}