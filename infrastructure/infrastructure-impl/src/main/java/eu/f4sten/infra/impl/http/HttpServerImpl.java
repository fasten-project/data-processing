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

import static eu.f4sten.infra.http.Scope.SINGLETON;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import eu.f4sten.infra.http.HttpServer;
import eu.f4sten.infra.http.Scope;
import eu.f4sten.infra.impl.InfraArgs;

public class HttpServerImpl implements HttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerImpl.class);

    private final Injector injector;
    private final InfraArgs args;
    private final Server server;

    private final Map<Class<?>, Scope> bindings = new HashMap<>();

    @Inject
    public HttpServerImpl(Injector injector, InfraArgs args) {
        this.injector = injector;
        this.args = args;
        server = new Server(args.httpPort);
    }

    @Override
    public void register(Class<?>... types) {
        register(SINGLETON, types);
    }

    @Override
    public void register(Scope scope, Class<?>... types) {
        for (var type : types) {
            bindings.put(type, scope);
        }
    }

    @Override
    public void start() {
        LOG.info("Starting HTTP Server ...");

        var config = new HttpServerConfig(injector, bindings);

        var ctx = new ServletContextHandler(NO_SESSIONS);
        ctx.setContextPath(args.httpBaseUrl);
        ctx.addServlet(new ServletHolder(new ServletContainer(config)), "/*");

        server.setHandler(ctx);

        try {
            server.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        LOG.info("Stopping HTTP Server ...");
        try {
            if (server != null && server.isRunning()) {
                server.stop();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}