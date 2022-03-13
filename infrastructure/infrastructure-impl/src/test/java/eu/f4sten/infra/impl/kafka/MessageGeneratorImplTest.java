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
package eu.f4sten.infra.impl.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.f4sten.infra.LoaderArgs;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.infra.utils.HostName;
import eu.f4sten.infra.utils.Version;

public class MessageGeneratorImplTest {

    private static final String SOME_ERROR = "some error";
    private static final String SOME_PLUGIN = "plugin";
    private static final String SOME_HOSTNAME = "hostname";
    private static final String SOME_VERSION = "0.1.2";
    private static final String SOME_PAYLOAD = "payload";
    private static final String SOME_INPUT = "input";

    private MessageGeneratorImpl sut;

    @BeforeEach
    public void setup() {
        var args = new LoaderArgs();
        args.plugin = SOME_PLUGIN;
        sut = new MessageGeneratorImpl(args, new TestHostName(), new TestVersion());
    }

    @Test
    public void assertTimeGetStd1() {
        var actual = sut.getStd(SOME_PAYLOAD);
        assertSimilarity(new Date(), actual.createdAt);
    }

    @Test
    public void assertTimeGetStd2() {
        var actual = sut.getStd(SOME_INPUT, SOME_PAYLOAD);
        assertSimilarity(new Date(), actual.createdAt);
    }

    @Test
    public void assertTimeGetErr() {
        var actual = sut.getErr(SOME_INPUT, new RuntimeException());
        assertSimilarity(new Date(), actual.createdAt);
    }

    @Test
    public void getStd1() {
        var actual = deleteCreatedAt(sut.getStd(SOME_PAYLOAD));
        var expected = new Message<Object, String>();
        expected.consumedAt = null;
        expected.createdAt = null;
        expected.error = null;
        expected.host = SOME_HOSTNAME;
        expected.input = null;
        expected.payload = SOME_PAYLOAD;
        expected.plugin = SOME_PLUGIN;
        expected.version = SOME_VERSION;
        assertEqualMsgs(expected, actual);
    }

    @Test
    public void getStd2() {
        var actual = deleteCreatedAt(sut.getStd(SOME_INPUT, SOME_PAYLOAD));
        var expected = new Message<Object, String>();
        expected.consumedAt = null;
        expected.createdAt = null;
        expected.error = null;
        expected.host = SOME_HOSTNAME;
        expected.input = SOME_INPUT;
        expected.payload = SOME_PAYLOAD;
        expected.plugin = SOME_PLUGIN;
        expected.version = SOME_VERSION;
        assertEqualMsgs(expected, actual);
    }

    @Test
    public void getErr() {
        var actual = deleteCreatedAt(sut.getErr(SOME_INPUT, new IllegalArgumentException(SOME_ERROR)));
        var expected = new Message<Object, String>();
        expected.consumedAt = null;
        expected.createdAt = null;
        expected.host = SOME_HOSTNAME;
        expected.input = SOME_INPUT;
        expected.payload = null;
        expected.plugin = SOME_PLUGIN;
        expected.version = SOME_VERSION;

        expected.error = new Message.Error();
        expected.error.type = IllegalArgumentException.class.getName();
        expected.error.message = SOME_ERROR;
        expected.error.stacktrace = null;

        assertNotNull(actual.error);
        var stack = actual.error.stacktrace;
        actual.error.stacktrace = null;
        assertEquals(expected, actual);

        assertTrue(stack.contains(IllegalArgumentException.class.getName()));
        assertTrue(stack.contains(SOME_ERROR));
        assertTrue(stack.contains("\n"));
        assertTrue(stack.contains("at " + MessageGeneratorImplTest.class.getName()));
    }

    private static <T, U> Message<T, U> deleteCreatedAt(Message<T, U> m) {
        m.createdAt = null;
        return m;
    }

    private static void assertSimilarity(Date e, Date a) {
        var diff = Math.abs(e.getTime() - a.getTime());
        assertTrue(diff < 500);
    }

    private void assertEqualMsgs(Object e, Object a) {
        assertEquals(e, a);
    }

    private static class TestHostName implements HostName {
        @Override
        public String get() {
            return SOME_HOSTNAME;
        }
    }

    private static class TestVersion implements Version {
        @Override
        public String get() {
            return SOME_VERSION;
        }
    }
}