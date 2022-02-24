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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.stefanbirkner.systemlambda.Statement;
import com.github.stefanbirkner.systemlambda.SystemLambda;

public class AssertArgsUtils {

    static final String TEXT_ERROR_INTRO = "Insufficient startup arguments";
    static final String TEXT_GENERIC_ERROR = "A requested argument is invalid";
    static final String TEXT_IS_NULL_ERROR = "A requested argument is null";
    static final String TEXT_INTRO_FOR_PARAMS = "The *subset* of related arguments that might get requested at runtime";

    public static void assertArgsValidation(String expectedHint, Statement s) {
        try {
            var out = SystemLambda.tapSystemOut(() -> {
                var e = assertThrows(Error.class, () -> {
                    s.execute();
                });
                // type unreachable, so testing for name
                boolean isAssertArgsError = "AssertArgsError".equals((e.getClass().getSimpleName()));
                assertTrue(isAssertArgsError);
            });

            boolean hasBasicParts = out.contains(TEXT_ERROR_INTRO) //
                    && out.contains(TEXT_INTRO_FOR_PARAMS) //
                    && (out.contains(TEXT_GENERIC_ERROR) || out.contains(TEXT_IS_NULL_ERROR));
            if (!hasBasicParts) {
                fail("Output does lacks basic validation text:\n" + out);
            }

            var wrappedHint = "(" + expectedHint + ")";
            if (!out.contains(wrappedHint)) {
                var sb = new StringBuilder();
                sb.append("String could not be found in output.\n");
                sb.append("Expected: ").append(wrappedHint).append("\n");
                sb.append("Captured:\n").append(out);
                fail(sb.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}