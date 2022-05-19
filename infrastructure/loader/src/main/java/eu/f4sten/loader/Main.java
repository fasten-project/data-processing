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

import static eu.fasten.core.utils.MemoryUsageUtils.logMaxMemory;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.google.inject.Guice;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.AssertArgsError;
import eu.f4sten.infra.InjectorConfig;
import eu.f4sten.infra.LoaderArgs;
import eu.f4sten.loader.utils.ArgsParser;
import eu.f4sten.loader.utils.LoggingUtils;
import eu.f4sten.loader.utils.ReflectionUtils;

public class Main {

    private Main() {
        // don't instantiate
    }

    public static void main(String[] rawArgs) {
        try {
            // setup logging
            var argsParser = new ArgsParser(rawArgs);
            var args = argsParser.parse(LoaderArgs.class);
            AssertArgs.notNull(args, a -> a.plugin, "no plugin defined");
            new LoggingUtils(args.logLevel);
            logger().info("Starting plugin {} ...", args.plugin);
            logMaxMemory();

            // find classes
            var ru = new ReflectionUtils("eu.f4sten", InjectorConfig.class, argsParser);
            var modules = ru.loadModules();
            var pluginClass = ru.findPluginClass(args.plugin);

            // setup injector and run requested plugin
            var injector = Guice.createInjector(modules);
            injector.getInstance(pluginClass).run();
        } catch (AssertArgsError e) {
            System.exit(1);
        } catch (Throwable t) {
            logger().warn("Throwable caught in main loader class, shutting down VM ...");
            t.printStackTrace();
            // make sure to tear down VM, including all running threads
            System.exit(1);
        }
    }

    private static Logger logger() {
        return getLogger(Main.class);
    }
}