/*
 * Copyright 2022 Software Improvement Group
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
package eu.f4sten.sourcesprovider.utils;

import eu.fasten.core.data.Constants;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static eu.fasten.core.utils.TestUtils.getTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayloadParsingTest {
    private PayloadParsing pp;

    @BeforeEach
    void setUp() {
        SourcesJarProvider sd = mock(SourcesJarProvider.class);
        when(sd.downloadSourcesJar(any(), any())).thenReturn("/test/path");
        pp = new PayloadParsing(sd);
    }

    @Test
    void findJavaPayloadTest() throws IOException, JSONException {
        var message = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/java_metadatadb_extension_message.json").toPath()));
        var payload = pp.findSourcePayload(message);
        assertNotNull(payload);
        assertEquals(Constants.mvnForge, payload.getForge());
        assertEquals("commons-codec:commons-codec", payload.getProduct());
        assertEquals("1.10", payload.getVersion());
        assertEquals("/test/path", payload.getSourcePath());
    }

    @Test
    void findPythonPayloadTest() throws IOException, JSONException {
        var message = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/python_metadatadb_extension_message.json").toPath()));
        var payload = pp.findSourcePayload(message);
        assertNotNull(payload);
        assertEquals(Constants.pypiForge, payload.getForge());
        assertEquals("pycg-stitch", payload.getProduct());
        assertEquals("0.0.7", payload.getVersion());
        assertEquals("/mnt/fasten/revision-callgraphs/pypi/pypi/sources/p/pycg-stitch/0.0.7", payload.getSourcePath());
    }

    @Test
    void findCPayloadTest() throws IOException, JSONException {
        var message = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_metadatadb_extension_message.json").toPath()));
        var payload = pp.findSourcePayload(message);
        assertNotNull(payload);
        assertEquals(Constants.debianForge, payload.getForge());
        assertEquals("anna", payload.getProduct());
        assertEquals("1.71", payload.getVersion());
        assertEquals("/mnt/fasten/revision-callgraphs/debian/sources/a/anna/1.71", payload.getSourcePath());
    }

    @Test
    void parseSourcePayloadTest() throws IOException, JSONException {
        var cSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_source_payload.json").toPath()));
        assertNotNull(pp.parse(cSourcePayload));

        var cNonSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_non_source_payload.json").toPath()));
        assertNull(pp.parse(cNonSourcePayload));

        var javaSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/java_source_payload.json").toPath()));
        assertNotNull(pp.parse(javaSourcePayload));

        var pythonSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/python_source_payload.json").toPath()));
        assertNotNull(pp.parse(pythonSourcePayload));
    }
}