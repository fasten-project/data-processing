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
package eu.f4sten.pomanalyzer.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import eu.fasten.core.maven.data.Exclusion;
import eu.fasten.core.maven.data.VersionConstraint;

public class CoreJacksonModule extends SimpleModule {

	private static final long serialVersionUID = 8302574258846915634L;

	public CoreJacksonModule() {

		addDependencyClasses();
	}

	@Override
	public String getModuleName() {
		return "Module for some core classes (Dependency, VersionConstraint, Exclusion)";
	}

	private void addDependencyClasses() {

		// Dependency.class itself works out of the box

		addSerializer(VersionConstraint.class, new JsonSerializer<VersionConstraint>() {
			@Override
			public void serialize(VersionConstraint value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException {
				gen.writeString(value.spec);
			}
		});
		addDeserializer(VersionConstraint.class, new JsonDeserializer<VersionConstraint>() {
			@Override
			public VersionConstraint deserialize(JsonParser p, DeserializationContext ctxt)
					throws IOException, JacksonException {
				return new VersionConstraint(p.getValueAsString());
			}
		});

		addSerializer(Exclusion.class, new JsonSerializer<Exclusion>() {
			@Override
			public void serialize(Exclusion value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException {
				gen.writeString(String.format("%s:%s", value.groupId, value.artifactId));
			}
		});
		addDeserializer(Exclusion.class, new JsonDeserializer<Exclusion>() {
			@Override
			public Exclusion deserialize(JsonParser p, DeserializationContext ctxt)
					throws IOException, JacksonException {
				String[] parts = p.getValueAsString().split(":");
				return new Exclusion(parts[0], parts[1]);
			}
		});
	}
}