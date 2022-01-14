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
package eu.f4sten.server.core;

public class Asserts {

    // TODO merge with fasten.core Asserts

    public static void assertNotNullOrEmpty(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalStateException("string is null or empty");
        }
    }

    public static void that(boolean b, String error) {
        if (!b) {
            throw new IllegalStateException(error);
        }
    }
}