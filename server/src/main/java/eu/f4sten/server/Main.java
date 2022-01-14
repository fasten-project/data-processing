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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Module;

import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.InjectorConfig;
import eu.f4sten.server.core.Plugin;

public class Main {

    private static final String FASTEN_BASE_PACKAGE = "eu.f4sten";

    // must be non-final, non-static or else the log-level will be ignored
    private Logger log;

    public static void main(String[] rawArgs) {
        setLogLevel(LogLevel.OFF);
        var args = parse(rawArgs, new ServerArgs());
        new Main().run(rawArgs, args);
    }

    private static Map<String, Class<?>> parameters = new HashMap<>();

    private static <T> T parse(String[] args, T parsedArgs) {
        var jc = JCommander.newBuilder() //
                .addObject(parsedArgs) //
                .acceptUnknownOptions(true) //
                .build();
        jc.parse(args);

        var c = parsedArgs.getClass();
        for (var f : jc.getFields().values()) {
            for (var n : f.getParameter().names()) {
                if (!parameters.containsKey(n)) {
                    parameters.put(n, parsedArgs.getClass());
                    continue;
                }
                var c2 = parameters.get(n);
                if (c.equals(c2)) {
                    // the same argsObj is being loaded again
                    continue;
                }

                log().error("The parameter '{}' is defined in both {} and {}", n, c, c2);
                System.exit(1);
            }
        }
        return parsedArgs;
    }

    public void run(String[] rawArgs, ServerArgs args) {
        // setup logging
        setLogLevel(args.logLevel);
        log = LoggerFactory.getLogger(Main.class);
        log.info("Starting plugin {} ...", args.plugin);

        // collect all modules for injection
        var modules = loadModules(rawArgs, args);
        var injector = Guice.createInjector(modules);

        // find corresponding plugin class
        Class<Plugin> c = findPluginClass(args.plugin);
        injector.getInstance(c).run();
    }

    private static void setLogLevel(LogLevel level) {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level.value);
    }

    private static List<Module> loadModules(String[] rawArgs, ServerArgs args) {
        var modules = new LinkedList<Module>();
        modules.add(new ServerConfig(args));

        log().info("Searching for @{} in package {} ...", InjectorConfig.class.getSimpleName(), FASTEN_BASE_PACKAGE);
        Reflections ref = new Reflections(FASTEN_BASE_PACKAGE);
        for (Class<?> cl : ref.getTypesAnnotatedWith(InjectorConfig.class)) {
            var m = loadModule(cl, rawArgs);
            if (m != null) {
                modules.add(m);
            }
        }
        return modules;
    }

    private static Module loadModule(Class<?> cl, String[] args) {
        log().info("Loading {} ...", cl);
        if (!IInjectorConfig.class.isAssignableFrom(cl)) {
            log().error("{} does not implement {}.", cl, IInjectorConfig.class);
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            var inits = (Constructor<Module>[]) cl.getConstructors();
            if (inits.length == 0 || inits.length > 1) {
                log().error("{} should have at most one constructor, but has {}.", cl, inits.length);
                return null;
            }
            var init = (Constructor<Module>) inits[0];

            if (init.getParameterCount() == 0) {
                String msg = "{} only has a no-args constructor. A custom argument class usually "
                        + "makes configuration easier and will be automatically injected to the "
                        + "constructor (and can then be bound and provided for other classes.";
                log().info(msg, cl);
                return init.newInstance();
            }

            if (init.getParameterCount() == 1) {
                var p = init.getParameters()[0];
                var argObj = initDefault(p.getType());
                if (argObj == null) {
                    return null;
                }
                parse(args, argObj);
                parse(args, argObj);
                return init.newInstance(argObj);
            }

            log().error("Configuration {} should have at most one parameter, but has {}.", cl, inits.length);
            return null;

        } catch (Exception e) {
            var msg = String.format("Failed to initialize {}.", cl);
            log().error(msg, e);
            return null;
        }
    }

    private static <T> T initDefault(Class<T> type) {
        try {
            var init = type.getConstructor();
            return init.newInstance();
        } catch (NoSuchMethodException e) {
            log().error("Parameter {} does not have an (implicit) default constructor.", type);
        } catch (Exception e) {
            var msg = String.format("Failed to initialize {}", type);
            log().error(msg, e);
        }
        return null;
    }

    private static Class<Plugin> findPluginClass(String plugin) {
        try {
            var c = Class.forName(plugin);
            if (!Plugin.class.isAssignableFrom(c)) {
                log().error("{} does not implement {}.", c, Plugin.class);
                System.exit(1);
            }
            @SuppressWarnings("unchecked")
            var pc = (Class<Plugin>) c;
            return pc;
        } catch (ClassNotFoundException e) {
            log().error("Class cannot be found: {}", plugin);
            System.exit(1);
        }
        return null;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(Main.class);
    }
}