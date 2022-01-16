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

import java.util.logging.Level;

public enum LogLevel {
    ALL("trace", Level.ALL),
    DEBUG("debug", Level.FINER),
    INFO("info", Level.CONFIG),
    WARN("warn", Level.WARNING),
    ERROR("error", Level.SEVERE),
    OFF("off", Level.OFF);

    public final String slf4j;
    public final Level jul;

    LogLevel(String slf4j, Level jul) {
        this.slf4j = slf4j;
        this.jul = jul;
    }
}