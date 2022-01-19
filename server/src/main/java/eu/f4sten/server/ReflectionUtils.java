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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;

import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.Plugin;

public class ReflectionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    private final String basePkg;
    private final Class<? extends Annotation> markerAnnotation;
    private final ArgsParser argsParser;

    public ReflectionUtils(String basePkg, Class<? extends Annotation> markerAnnotation, ArgsParser argsParser) {
        this.basePkg = basePkg;
        this.markerAnnotation = markerAnnotation;
        this.argsParser = argsParser;
    }

    public List<Module> loadModules(ServerArgs serverArgs) {
        var modules = new LinkedList<Module>();
        modules.add(new ServerConfig(serverArgs));

        LOG.info("Searching for @{} in package {} ...", markerAnnotation.getSimpleName(), basePkg);
        Reflections ref = new Reflections(basePkg);
        for (Class<?> cl : ref.getTypesAnnotatedWith(markerAnnotation)) {
            var m = loadModule(cl, argsParser);
            if (m != null) {
                modules.add(m);
            }
        }
        return modules;
    }

    private static Module loadModule(Class<?> cl, ArgsParser args) {
        LOG.info("Loading {} ...", cl);
        if (!IInjectorConfig.class.isAssignableFrom(cl)) {
            LOG.error("{} does not implement {}.", cl, IInjectorConfig.class);
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            var inits = (Constructor<Module>[]) cl.getConstructors();
            if (inits.length == 0 || inits.length > 1) {
                LOG.error("{} should have at most one constructor, but has {}.", cl, inits.length);
                return null;
            }
            var init = (Constructor<Module>) inits[0];

            if (init.getParameterCount() == 0) {
                String msg = "{} only has a no-args constructor. A custom argument class usually "
                        + "makes configuration easier and will be automatically injected to the "
                        + "constructor (and can then be bound and provided for other classes).";
                LOG.info(msg, cl);
                return init.newInstance();
            }

            if (init.getParameterCount() == 1) {
                var paramType = init.getParameters()[0].getType();
                var argObj = args.parse(paramType);
                return init.newInstance(argObj);
            }

            LOG.error("Configuration {} should have at most one parameter, but has {}.", cl, inits.length);
            return null;

        } catch (Exception e) {
            var msg = String.format("Failed to initialize {}.", cl);
            LOG.error(msg, e);
            return null;
        }
    }

    public Class<Plugin> findPluginClass(String plugin) {
        try {
            var c = Class.forName(plugin);
            if (!Plugin.class.isAssignableFrom(c)) {
                LOG.error("{} does not implement {}.", c, Plugin.class);
                System.exit(1);
            }
            @SuppressWarnings("unchecked")
            var pc = (Class<Plugin>) c;
            return pc;
        } catch (ClassNotFoundException e) {
            LOG.error("Class cannot be found: {}", plugin);
            System.exit(1);
        }
        return null;
    }
}