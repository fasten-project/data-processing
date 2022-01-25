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
package eu.f4sten.test;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;

import uk.org.lidalia.slf4jext.Level;

public class TestLoggerUtils {

    private static TestLogger getLogger(Class<?> c) {
        return TestLoggerFactory.getTestLogger(c);
    }

    public static void clearLog() {
        TestLoggerFactory.clearAll();
    }

    public static List<String> getFormattedLogs(Class<?> c) {
        return getLogger(c).getAllLoggingEvents().stream() //
                .map(e -> e.getLevel() + " " + e.getFormattedMessage()) //
                .collect(Collectors.toList());
    }

    public static void assertLogsContain(Class<?> c, String msg, Object... args) {
        var logs = getFormattedLogs(c);
        var expected = args.length > 0 ? format(msg, args) : msg;
        if (!logs.contains(expected)) {
            var sb = new StringBuilder();
            sb.append("Expected log line not found. Expected:\n    ");
            sb.append(expected);
            sb.append("\nRecorded: [");
            for (var l : logs) {
                sb.append("\n    ").append(l);
            }
            sb.append("\n]");
            fail(sb.toString());
        }
    }

    public static void enableTerminalLogging() {
        TestLoggerFactory.getInstance().setPrintLevel(Level.TRACE);
    }
}