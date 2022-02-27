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
package eu.f4sten.depgraph.data;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import eu.f4sten.depgraph.endpoints.Hello;

public class JsonSerializationModule extends SimpleModule {

    private static final long serialVersionUID = -8680812356142473495L;

    public JsonSerializationModule() {

        addSerializer(Hello.class, new JsonSerializer<Hello>() {
            @Override
            public void serialize(Hello h, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(h.name);
            }
        });

        addDeserializer(Hello.class, new JsonDeserializer<Hello>() {
            @Override
            public Hello deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                String name = p.getValueAsString();
                // bad example, Hello is not a data structure and has dependencies
                return new Hello(new Naming(name), null);
            }
        });
    }
}