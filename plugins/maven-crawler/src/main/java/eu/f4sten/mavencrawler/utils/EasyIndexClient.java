/*
 * Copyright 2022 Delft University of Technology
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
package eu.f4sten.mavencrawler.utils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import org.glassfish.jersey.client.ClientConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.c0ps.maveneasyindex.Artifact;
import dev.c0ps.maveneasyindex.ArtifactModule;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ContextResolver;

public class EasyIndexClient {

    private final Client client;
    private final String serverUrl;

    public EasyIndexClient(@Named("EasyIndexClient.serverUrl") String serverUrl) {
        this.serverUrl = serverUrl;
        this.client = setupClient();
    }

    private Client setupClient() {
        var om = new ObjectMapper().registerModule(new ArtifactModule());
        var config = new ClientConfig().register(new ContextResolver<ObjectMapper>() {
            @Override
            public ObjectMapper getContext(Class<?> type) {
                return om;
            }
        });
        return ClientBuilder.newClient(config);
    }

    public boolean exists(int i) {
        var r = client //
                .target(serverUrl).path("/exists/" + i) //
                .request(APPLICATION_JSON) //
                .get();
        int s = r.getStatus();
        int t = Status.OK.getStatusCode();
        return s == t;
    }

    public List<Artifact> get(int i) {
        return client.target(serverUrl).path("/get/" + i) //
                .request(APPLICATION_JSON) //
                .get(new GenericType<List<Artifact>>() {});
    }
}