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
package eu.f4sten.server.core.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import eu.f4sten.server.core.kafka.Message.Error;

public class MessageTest {

	private static final Date SOME_DATE = new Date();
	private static final Date SOME_OTHER_DATE = new Date();

	@Test
	public void errorDefaults() {
		var sut = new Message.Error();
		assertNull(sut.message);
		assertNull(sut.stacktrace);
		assertNull(sut.type);
	}

	@Test
	public void errorEqualityDefault() {
		var a = new Message.Error();
		var b = new Message.Error();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void errorEqualityNonDefault() {
		var a = someError();
		var b = someError();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void errorEqualityDiffMessage() {
		var a = new Message.Error();
		var b = new Message.Error();
		b.message = "m";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void errorEqualityDiffStacktrace() {
		var a = new Message.Error();
		var b = new Message.Error();
		b.stacktrace = "s";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void errorEqualityDiffType() {
		var a = new Message.Error();
		var b = new Message.Error();
		b.type = "t";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void errorHasToString() {
		var actual = new Message.Error().toString();
		assertTrue(actual.contains(Message.Error.class.getSimpleName()));
		assertTrue(actual.contains("@"));
		assertTrue(actual.contains("\n"));
		assertTrue(actual.contains("message"));
	}

	@Test
	public void defaults() {
		var sut = new Message<String, String>();
		assertNotNull(sut.consumedAt);
		assertNotNull(sut.createdAt);
		assertNull(sut.error);
		assertNull(sut.version);
		assertNull(sut.host);
		assertNull(sut.input);
		assertNull(sut.payload);
		assertNull(sut.plugin);
	}

	@Test
	public void equalityDefaults() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityNonDefaults() {
		var a = someMessage();
		var b = someMessage();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffConsumedAt() {
		var a = new Message<String, String>();
		a.consumedAt = SOME_DATE;
		var b = new Message<String, String>();
		b.consumedAt = new Date();
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffCreatedAt() {
		var a = new Message<String, String>();
		a.createdAt = SOME_DATE;
		var b = new Message<String, String>();
		b.createdAt = new Date();
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffError() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.error = someError();
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffVersion() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.version = "f";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffHost() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.host = "h";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffInput() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.input = "i";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffPayload() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.payload = "p";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffPlugin() {
		var a = new Message<String, String>();
		var b = new Message<String, String>();
		b.plugin = "p";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void hasToString() {
		var actual = new Message<String, String>().toString();
		assertTrue(actual.contains(Message.class.getSimpleName()));
		assertTrue(actual.contains("@"));
		assertTrue(actual.contains("\n"));
		assertTrue(actual.contains("createdAt"));
	}

	private Message<String, String> someMessage() {
		var m = new Message<String, String>();
		m.consumedAt = SOME_OTHER_DATE;
		m.createdAt = SOME_DATE;
		m.error = someError();
		m.version = "v";
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