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
package eu.f4sten.loader.utils;

import java.util.logging.LogManager;

import org.slf4j.bridge.SLF4JBridgeHandler;

import eu.f4sten.infra.LogLevel;

public class LoggingUtils {

    public LoggingUtils(LogLevel level) {
        setLogLevel(level);
    }

    public void setLogLevel(LogLevel level) {
        // slf4j-simple
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level.slf4j);
        System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss.SSS");

        // jul
        var lm = LogManager.getLogManager();
        lm.reset();
        var root = lm.getLogger("");
        root.setLevel(level.jul); // avoid bridging all levels(!)
        root.addHandler(new SLF4JBridgeHandler());
    }
}