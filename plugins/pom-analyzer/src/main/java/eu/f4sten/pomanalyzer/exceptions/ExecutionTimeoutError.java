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
package eu.f4sten.pomanalyzer.exceptions;

public class ExecutionTimeoutError extends Error {

    private static final long serialVersionUID = -8751727696511990784L;

    public ExecutionTimeoutError(String msg) {
        super(msg);
    }

    public ExecutionTimeoutError(Throwable t) {
        super(t);
    }

    public ExecutionTimeoutError(String msg, Throwable t) {
        super(msg, t);
    }
}