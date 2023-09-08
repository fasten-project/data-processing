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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    public void pluginCanBeStarted() {
        assertFalse(TestPlugin.wasCalled);
        Main.main(new String[] { "--run", TestPlugin.class.getName() });
        assertTrue(TestPlugin.wasCalled);
        TestPlugin.wasCalled = false;
    }

    public static class TestPlugin implements Runnable {

        public static boolean wasCalled = false;

        @Override
        public void run() {
            wasCalled = true;
        }
    }
}