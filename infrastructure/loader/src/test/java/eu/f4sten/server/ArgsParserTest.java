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
package eu.f4sten.server;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.Parameter;

public class ArgsParserTest {

    @Test
    public void defaultsWork() {
        var actual = with().parse(TestArgsA.class);
        assertEquals(123, actual.a);
    }

    @Test
    public void parsingWorks() {
        var actual = with("-a", "2").parse(TestArgsA.class);
        assertEquals(2, actual.a);
    }

    @Test
    public void argsNeedNoArgsConstructor() {
        var e = assertThrows(IllegalStateException.class, () -> {
            with().parse(TestArgsNoNoArgsConstructor.class);
        });
        var msg = format("Argument parsing class %s does not have an (implicit) default constructor.", //
                TestArgsNoNoArgsConstructor.class.getName());
        assertEquals(msg, e.getMessage());
    }

    @Test
    public void argsNeedWorkingConstructor() {
        var e = assertThrows(IllegalStateException.class, () -> {
            with().parse(TestArgsCrashingConstructor.class);
        });
        var msg = format("Default constructor of %s failed with %s: %s", //
                TestArgsCrashingConstructor.class.getName(), //
                RuntimeException.class.getName(), //
                "x");
        assertEquals(msg, e.getMessage());
    }

    @Test
    public void multipleArgsCanBeParsed() {
        var sut = with();
        sut.parse(TestArgsA.class);
        var b = sut.parse(TestArgsB.class);
        assertEquals(345, b.b);
    }

    @Test
    public void sameArgsCanBeParsedTwice() {
        var sut = with();
        sut.parse(TestArgsA.class);
        sut.parse(TestArgsA.class);
    }

    @Test
    public void argsCannotHaveDuplicatedParameters() {
        var sut = with();
        sut.parse(TestArgsA.class);
        var e = assertThrows(IllegalStateException.class, () -> {
            sut.parse(TestArgsA2.class);
        });
        var msg = format("The parameter -a is defined in both %s and %s", //
                TestArgsA2.class.getName(), //
                TestArgsA.class.getName());
        assertEquals(msg, e.getMessage());
    }

    public static class TestArgsA {
        @Parameter(names = "-a", arity = 1)
        public int a = 123;
    }

    public static class TestArgsA2 {
        @Parameter(names = "-a", arity = 1)
        public int a = 234;
    }

    public static class TestArgsB {
        @Parameter(names = "-b", arity = 1)
        public int b = 345;
    }

    public static class TestArgsNoNoArgsConstructor {
        public TestArgsNoNoArgsConstructor(int a) {}
    }

    public static class TestArgsCrashingConstructor {
        public TestArgsCrashingConstructor() {
            throw new RuntimeException("x");
        }
    }

    private ArgsParser with(String... args) {
        return new ArgsParser(args);
    }
}