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
package eu.f4sten.infra.impl.http;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;

import eu.f4sten.infra.http.Scope;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.ContextResolver;

public class HttpServerConfig extends ResourceConfig {

    private static final Map<Scope, Class<? extends Annotation>> SCOPE_ANNOTATIONS = new HashMap<>();

    {
        SCOPE_ANNOTATIONS.put(Scope.PROTOTYPE, PerLookup.class);
        SCOPE_ANNOTATIONS.put(Scope.REQUEST, RequestScoped.class);
        SCOPE_ANNOTATIONS.put(Scope.SINGLETON, Singleton.class);
    }

    public HttpServerConfig(Injector injector, Map<Class<?>, Scope> bindings) {

        // register global ObjectMapper
        var om = injector.getInstance(ObjectMapper.class);
        register(new ObjectMapperProvider(om));

        // register all resources
        for (var type : bindings.keySet()) {
            register(type);
        }

        // register factory for all resources
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                for (var type : bindings.keySet()) {
                    var scope = bindings.get(type);
                    var factory = new ServiceFactory<>(injector, type);
                    bindFactory(factory).to(type).in(SCOPE_ANNOTATIONS.get(scope));
                }
            }
        });
    }

    private static class ServiceFactory<T> implements Supplier<T> {

        private Injector injector;
        private Class<T> type;

        private ServiceFactory(Injector injector, Class<T> type) {
            this.injector = injector;
            this.type = type;
        }

        @Override
        public T get() {
            return injector.getInstance(type);
        }
    }

    private static class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

        private final ObjectMapper om;

        private ObjectMapperProvider(ObjectMapper om) {
            this.om = om;
        }

        @Override()
        public ObjectMapper getContext(final Class<?> type) {
            return om;
        }
    }
}