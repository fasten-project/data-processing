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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.http.HttpServer;

public class HttpServerGracefulShutdownThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerGracefulShutdownThread.class);

    private HttpServer server;

    public HttpServerGracefulShutdownThread(HttpServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        LOG.info("Gracefully stopping HTTP server ...");
        server.stop();
    }
}