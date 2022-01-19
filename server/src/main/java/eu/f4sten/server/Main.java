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

import static org.slf4j.LoggerFactory.getLogger;

import com.google.inject.Guice;

import eu.f4sten.server.core.InjectorConfig;

public class Main {

    private Main() {
        // don't instantiate
    }

    public static void main(String[] rawArgs) {

        // setup logging
        var argsParser = new ArgsParser(rawArgs);
        var args = argsParser.parse(ServerArgs.class);
        new LoggingUtils(args.logLevel);
        getLogger(Main.class).info("Starting plugin {} ...", args.plugin);

        // find classes
        var ru = new ReflectionUtils("eu.f4sten", InjectorConfig.class, argsParser);
        var modules = ru.loadModules(args);
        var pluginClass = ru.findPluginClass(args.plugin);

        // setup injector and run server
        var injector = Guice.createInjector(modules);
        injector.getInstance(pluginClass).run();
    }
}