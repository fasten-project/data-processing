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
package eu.f4sten.loader;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.stefanbirkner.systemlambda.SystemLambda;

import eu.f4sten.infra.Plugin;

public class MainTest {

    @Test
    public void pluginCanBeStarted() {
        assertFalse(TestPlugin.wasCalled);
        Main.main(new String[] { "--plugin", TestPlugin.class.getName() });
        assertTrue(TestPlugin.wasCalled);
        TestPlugin.wasCalled = false;
    }

    @Test
    public void missingPluginPrintsUsage() throws Exception {
        var out = SystemLambda.tapSystemOut(() -> {
            catchSystemExit(() -> {
                Main.main(new String[] {});
            });
        });
        assertTrue(out.contains("Insufficient startup arguments"));
        assertTrue(out.contains("no plugin defined"));
        assertTrue(out.contains("The *subset* of related arguments"));
        assertTrue(out.contains("--plugin"));
    }

    public static class TestPlugin implements Plugin {

        public static boolean wasCalled = false;

        @Override
        public void run() {
            wasCalled = true;
        }
    }
}