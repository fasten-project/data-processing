package eu.f4sten.sourcesprovider.utils;

import eu.f4sten.sourcesprovider.data.MavenSourcePayload;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static eu.fasten.core.utils.TestUtils.getTestResource;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadParsingTest {

    @Test
    void findPayloadTest() {
    }

    @Test
    void isSourcePayloadTest() throws IOException, JSONException {
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