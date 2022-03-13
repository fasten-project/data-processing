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
package eu.f4sten.infra.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.Date;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.f4sten.infra.kafka.Message;
import eu.f4sten.infra.kafka.Message.Error;
import eu.fasten.core.json.ObjectMapperBuilder;

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
        // message does not need a special serializer
    }

    @Test
    public void messagesCanBeDeSerialized() throws JsonProcessingException {
        var in = someMessage();
        String json = om.writeValueAsString(in);
        var out = om.readValue(json, new TRef<Message<String, String>>() {});
        assertEquals(in, out);
    }

    @Test
    public void serializationDoesNotCrashWhenNotAllInfoIsConsumed() throws JsonProcessingException {
        var in = someMessage();
        String json = om.writeValueAsString(in);
        var out = om.readValue(json, new TRef<Message<Void, String>>() {});
        assertNull(out.input);
        assertEquals(in.payload, out.payload);
    }

    @Test
    public void formatIsCompatibleWithLegacyMessages() throws JsonMappingException, JsonProcessingException {

        var createdAt = 1642127882997L;
        var comsumedAt = 1642127889927L;
        var oldJson = "{\n" //
                + "  \"createdAt\": " + createdAt + ",\n" //
                + "  \"consumedAt\": " + comsumedAt + ",\n" //
                + "  \"host\": \"a68efd2412d9\",\n" //
                + "  \"plugin\": \"X\",\n" //
                + "  \"version\": \"0.1.2\",\n" //
                + "  \"input\": \"i\",\n" //
                + "  \"payload\": \"p\"\n" //
                + "}";

        // read
        var actual = om.readValue(oldJson, new TRef<Message<String, String>>() {});
        var expected = new Message<String, String>();
        expected.createdAt = toDate(createdAt);
        expected.consumedAt = toDate(comsumedAt);
        expected.host = "a68efd2412d9";
        expected.plugin = "X";
        expected.version = "0.1.2";
        expected.input = "i";
        expected.payload = "p";
        assertEquals(expected, actual);

        // write
        var newJson = om.writeValueAsString(actual);
        assertJsonEquals(oldJson, newJson);
    }

    @Test
    public void formatIsCompatibleWithLegacyErrors() throws JsonMappingException, JsonProcessingException {
        var oldJson = "{\n" //
                + "  \"error\": {\n" //
                + "    \"type\": \"1\",\n" //
                + "    \"message\": \"2\",\n" //
                + "    \"stacktrace\": \"3\"\n" //
                + "  }\n" //
                + "}";

        // read
        var m = om.readValue(oldJson, new TRef<Message<String, String>>() {});
        var actual = m.error;
        var expected = new Message.Error();
        expected.type = "1";
        expected.message = "2";
        expected.stacktrace = "3";
        assertEquals(expected, actual);

        // write
        var newJson = om.writeValueAsString(m);
        assertJsonEquals(oldJson, newJson);
    }

    private Message<String, String> someMessage() {
        var m = new Message<String, String>();
        m.consumedAt = new Date();
        m.createdAt = new Date();
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

    private static Date toDate(long comsumedAt) {
        return new Date(new Timestamp(comsumedAt).getTime());
    }

    private void assertJsonEquals(String expectedJson, String actualJson) {
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT_ORDER);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}