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
package eu.f4sten.pomanalyzer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MavenIdTest {

	@Test
	public void defaultValues() {
		var sut = new MavenId();
		assertNull(sut.groupId);
		assertNull(sut.artifactId);
		assertNull(sut.version);
		assertNull(sut.artifactRepository);
	}

	@Test
	public void equalityDefault() {
		var a = new MavenId();
		var b = new MavenId();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityNonDefault() {
		var a = someId();
		var b = someId();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffGroup() {
		var a = new MavenId();
		var b = new MavenId();
		b.groupId = "x";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffArtifact() {
		var a = new MavenId();
		var b = new MavenId();
		b.artifactId = "x";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffVersion() {
		var a = new MavenId();
		var b = new MavenId();
		b.version = "x";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalityDiffArtifactRepository() {
		var a = new MavenId();
		var b = new MavenId();
		b.artifactRepository = "x";
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void hasToString() {
		var actual = new MavenId().toString();
		assertTrue(actual.contains("\n"));
		assertTrue(actual.contains(MavenId.class.getSimpleName()));
		assertTrue(actual.contains("artifactRepository"));
	}

	private MavenId someId() {
		var id = new MavenId();
		id.groupId = "g";
		id.artifactId = "a";
		id.version = "v";
		id.artifactRepository = "ar";
		return id;
	}
}