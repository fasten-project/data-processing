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
package eu.f4sten.examples.data;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SomeInputDataJson {

    public static class SomeInputDataDeserializer extends JsonDeserializer<SomeInputData> {

        @Override
        public SomeInputData deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JacksonException {
            var parts = p.getValueAsString().split(":");
            SomeInputData t = new SomeInputData();
            t.input = parts[0];
            t.time = new Date(Long.valueOf(parts[1]));
            return t;
        }
    }

    public static class SomeInputDataSerializer extends JsonSerializer<SomeInputData> {

        @Override
        public void serialize(SomeInputData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            var time = value.time.getTime();
            gen.writeString(value.input + ":" + time);
        }
    }
}