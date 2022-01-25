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

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static eu.f4sten.test.TestLoggerUtils.assertLogsContain;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Binder;

import eu.f4sten.server.ReflectionUtilsTest.M6.Args;
import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.Plugin;
import eu.f4sten.test.TestLoggerUtils;

public class ReflectionUtilsTest {

    private static final String BASE_PKG = ReflectionUtils.class.getPackageName();

    private ArgsParser argsParser;
    private ReflectionUtils sut;

    @BeforeEach
    public void setup() {
        TestLoggerUtils.clearLog();
        argsParser = mock(ArgsParser.class);
        when(argsParser.parse(Args.class)).thenReturn(new Args());
        sut = new ReflectionUtils(BASE_PKG, NonExisting.class, argsParser);
    }

    public Set<IInjectorConfig> loadModules(Class<? extends Annotation> a) {
        return new ReflectionUtils(BASE_PKG, a, argsParser).loadModules();
    }

    @Test
    public void findPlugin() {
        var expected = ExamplePlugin.class;
        var actual = sut.findPluginClass(expected.getName());
        assertEquals(expected, actual);
    }

    @Test
    public void findNonPlugin() throws Exception {
        catchSystemExit(() -> {
            sut.findPluginClass(String.class.getName());
        });
        assertLogsContain(ReflectionUtils.class, //
                "ERROR Class %s does not implement %s", //
                String.class.getName(), //
                Plugin.class.getName());
    }

    @Test
    public void findNonExisting() throws Exception {
        String nonExisting = "some.non.existing.Class";
        catchSystemExit(() -> {
            sut.findPluginClass(nonExisting);
        });
        assertLogsContain(ReflectionUtils.class, //
                "ERROR Class cannot be found: %s", //
                nonExisting);
    }

    @Test
    public void notFindingModulesDoesNotCrash() throws Exception {
        assertEmptySet(loadModules(NonExisting.class));
        assertLogs( //
                format("INFO Searching for @%s in package %s ...", //
                        NonExisting.class.getSimpleName(), //
                        ReflectionUtilsTest.class.getPackageName()));
    }

    @Test
    public void moduleDoesNotImplementInterface() throws Exception {
        assertEmptySet(loadModules(DoesNotImplement.class));
        assertLogs( //
                format("INFO Searching for @%s in package %s ...", //
                        DoesNotImplement.class.getSimpleName(), //
                        ReflectionUtilsTest.class.getPackageName()), //
                format("INFO Loading %s ...", M1.class.getName()), //
                format("ERROR Class %s does not implement %s", M1.class.getName(), IInjectorConfig.class.getName()));
    }

    @Test
    public void moduleHasTooManyConstructors() throws Exception {
        assertEmptySet(loadModules(TooManyConstructors.class));
        TestLoggerUtils.assertLogsContain(ReflectionUtils.class, //
                "ERROR %s should have at most one constructor, but has 2", //
                M2.class.getName());
    }

    @Test
    public void moduleWithNoArgsConstructorWorksWithInfo() throws Exception {
        var actual = loadModules(OnlyNoArgsConstructor.class);
        assertTrue(actual.size() == 1);
        assertTrue(actual.iterator().next() instanceof M3);

        String msg = "INFO %s only has a no-args constructor. A custom argument class usually "
                + "makes configuration easier and will be automatically injected to the "
                + "constructor (and can then be bound and provided for other classes).";

        TestLoggerUtils.assertLogsContain(ReflectionUtils.class, //
                msg, //
                M3.class.getName());
    }

    @Test
    public void moduleWithTooManyArgsConstructor() throws Exception {
        assertEmptySet(loadModules(TooManyArgsConstructor.class));
        TestLoggerUtils.assertLogsContain(ReflectionUtils.class, //
                "ERROR Constructor of %s should have at most one parameter, but has 2", //
                M4.class.getName());
    }

    @Test
    public void moduleInitFailsException() throws Exception {
        assertEmptySet(loadModules(InitCrashes.class));
        TestLoggerUtils.assertLogsContain(ReflectionUtils.class, //
                "ERROR Construction of %s failed with %s: x", //
                M5.class.getName(), //
                RuntimeException.class.getName());
    }

    @Test
    public void moduleFullExample() throws Exception {
        var mods = loadModules(FullExample.class);
        assertTrue(mods.size() == 1);
        var actual = (M6) mods.iterator().next();
        assertNotNull(actual.args);
    }

    private static void assertEmptySet(Set<?> actual) {
        var expected = Set.of();
        assertEquals(expected, actual);
    }

    private static void assertLogs(String... msgs) {
        for (var msg : msgs) {
            TestLoggerUtils.assertLogsContain(ReflectionUtils.class, msg);
        }
    }

    // test classes and annotations

    public static class ExamplePlugin implements Plugin {
        @Override
        public void run() {}
    }

    @interface NonExisting {}

    @interface DoesNotImplement {}

    @DoesNotImplement
    public static class M1 {}

    @interface TooManyConstructors {}

    @TooManyConstructors
    public static class M2 extends BaseModule {
        public M2() {}

        public M2(int i) {}
    }

    @interface OnlyNoArgsConstructor {}

    @OnlyNoArgsConstructor
    public static class M3 extends BaseModule {
        public M3() {}
    }

    @interface TooManyArgsConstructor {}

    @TooManyArgsConstructor
    public static class M4 extends BaseModule {
        public M4(int a, int b) {}
    }

    @interface InitCrashes {}

    @InitCrashes
    public static class M5 extends BaseModule {
        public M5() {
            throw new RuntimeException("x");
        }
    }

    @interface FullExample {}

    @FullExample
    public static class M6 extends BaseModule {
        public Args args;

        public M6(Args args) {
            this.args = args;
        }

        public static class Args {}
    }

    public abstract static class BaseModule implements IInjectorConfig {
        @Override
        public void configure(Binder binder) {}
    }
}