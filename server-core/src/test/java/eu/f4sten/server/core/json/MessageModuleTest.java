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
package eu.f4sten.server.core.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.f4sten.server.core.kafka.Message;
import eu.f4sten.server.core.kafka.Message.Error;

public class MessageModuleTest {

	private ObjectMapper om;

	@BeforeEach
	public void setup() {
		om = new ObjectMapperBuilder() {
			@Override
			protected ObjectMapper addMapperOptions(ObjectMapper om) {
				return om.enable(SerializationFeature.INDENT_OUTPUT);
			}
		}.build();
	}

	@Test
	public void messagesCanBeDeSerialized() throws JsonProcessingException {
		var in = someMessage();
		String json = om.writeValueAsString(in);
		var out = om.readValue(json, new TypeReference<Message<String, String>>() {
		});
		assertEquals(in, out);
	}

	@Test
	public void serializationDoesNotCrashWhenNotAllInfoIsConsumed() throws JsonProcessingException {
		var in = someMessage();
		String json = om.writeValueAsString(in);
		var out = om.readValue(json, new TypeReference<Message<Void, String>>() {
		});
		assertNull(out.input);
		assertEquals(in.payload, out.payload);
	}

	private Message<String, String> someMessage() {
		var m = new Message<String, String>();
		m.createdAt = new Date();
		m.error = someError();
		m.fastenVersion = "v";
		m.host = "h";
		m.input = "i";
		m.payload = "p";
		m.plugin = "pl";
		return m;
	}

	private Error someError() {
		var e = new Message.Error();
		e.message = "m";
		e.stacktrace = "s";
		e.type = "t";
		return e;
	}
}