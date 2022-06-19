package eu.f4sten.sourcesprovider.utils;

import eu.f4sten.sourcesprovider.data.MavenSourcePayload;
import eu.fasten.core.data.Constants;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static eu.fasten.core.utils.TestUtils.getTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadParsingTest {

    @Test
    void findJavaPayloadTest() throws IOException, JSONException {
        var mvnSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/java_metadatadb_extension_message.json").toPath()));
        assertThrows(UnsupportedOperationException.class, () -> {
            PayloadParsing.findSourcePayload(mvnSourcePayload);
        });
//        var payload = PayloadParsing.findSourcePayload(mvnSourcePayload);
//        assertNotNull(payload);
//        assertTrue(payload instanceof MavenSourcePayload);
//        var mavenPayload = (MavenSourcePayload) payload;
//        assertEquals(Constants.mvnForge, mavenPayload.getForge());
//        assertEquals("commons-codec:commons-codec", mavenPayload.getProduct());
//        assertEquals("commons-codec", mavenPayload.getGroupId());
//        assertEquals("commons-codec", mavenPayload.getArtifactId());
//        assertEquals("1.10", mavenPayload.getVersion());
//        assertEquals("https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.10/commons-codec-1.10-sources.jar", mavenPayload.getSourcesUrl());
    }

    @Test
    void findPythonPayloadTest() throws IOException, JSONException {
        var sourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/python_metadatadb_extension_message.json").toPath()));
        var payload = PayloadParsing.findSourcePayload(sourcePayload);
        assertNotNull(payload);
        assertEquals(Constants.pypiForge, payload.getForge());
        assertEquals("pycg-stitch", payload.getProduct());
        assertEquals("0.0.7", payload.getVersion());
        assertEquals("/mnt/fasten/revision-callgraphs/pypi/pypi/sources/p/pycg-stitch/0.0.7", payload.getSourcePath());
    }

    @Test
    void findCPayloadTest() throws IOException, JSONException {
        var sourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_metadatadb_extension_message.json").toPath()));
        var payload = PayloadParsing.findSourcePayload(sourcePayload);
        assertNotNull(payload);
        assertEquals(Constants.debianForge, payload.getForge());
        assertEquals("anna", payload.getProduct());
        assertEquals("1.71", payload.getVersion());
        assertEquals("/mnt/fasten/revision-callgraphs/debian/sources/a/anna/1.71", payload.getSourcePath());
    }

    @Test
    void parseSourcePayloadTest() throws IOException, JSONException {
        var cSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_source_payload.json").toPath()));
        assertNotNull(PayloadParsing.parse(cSourcePayload));

        var cNonSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/c_non_source_payload.json").toPath()));
        assertNull(PayloadParsing.parse(cNonSourcePayload));

        var javaSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/java_source_payload.json").toPath()));
        assertTrue(PayloadParsing.parse(javaSourcePayload) instanceof MavenSourcePayload);

        var pythonSourcePayload = new JSONObject(Files.readString(getTestResource("PayloadParsingTest/python_source_payload.json").toPath()));
        assertNotNull(PayloadParsing.parse(pythonSourcePayload));
    }
}