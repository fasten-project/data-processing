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
package eu.f4sten.vulchainfinder.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.fasten.core.data.FastenJavaURI;
import eu.fasten.core.data.FastenURI;
import java.io.IOException;

public class FastenURIJacksonModule extends SimpleModule {

    public FastenURIJacksonModule() {

        addSerializer(FastenURI.class, new JsonSerializer<>() {
            @Override
            public void serialize(FastenURI value, JsonGenerator gen,
                                  SerializerProvider serializer) throws IOException {
                gen.writeString(value.toString());
            }
        });

        addDeserializer(FastenURI.class, new JsonDeserializer<>() {
            @Override
            public FastenURI deserialize(JsonParser p,
                                         DeserializationContext deserializationContext)
                throws IOException {
                final var uri = p.getValueAsString();
                FastenURI fastenURI;
                if (uri.startsWith("fasten://mvn")) {
                    fastenURI = FastenJavaURI.create(uri);
                }else {
                    fastenURI = FastenURI.create(uri);
                }
                return fastenURI;
            }
        });
    }
}