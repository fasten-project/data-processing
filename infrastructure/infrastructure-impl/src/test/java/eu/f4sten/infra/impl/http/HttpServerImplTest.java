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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Injector;

import eu.f4sten.infra.http.Scope;
import eu.f4sten.infra.impl.InfraArgs;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class HttpServerImplTest {

    private static final String SERVER_URL = "http://localhost:8080";

    private Injector injector;
    private InfraArgs args;
    private HttpServerImpl sut;
    private TestResource test;
    private ObjectMapper om;

    @BeforeEach
    public void setup() {
        args = new InfraArgs();
        injector = mock(Injector.class);
        sut = new HttpServerImpl(injector, args);

        om = JsonMapper.builder().build().registerModule(new TestModule());
        when(injector.getInstance(ObjectMapper.class)).thenReturn(om);

        test = null;
        when(injector.getInstance(TestResource.class)).thenAnswer(new Answer<TestResource>() {
            @Override
            public TestResource answer(InvocationOnMock invocation) throws Throwable {
                test = new TestResource();
                return test;
            }
        });
    }

    @AfterEach
    public void teardown() {
        sut.stop();
    }

    @Test
    public void scopeDefaultIsSingleton() {
        sut.register(TestResource.class);
        startAndWait(sut);
        get("/");
        get("/");
        assertEquals(2, test.counter);
        verify(injector, times(1)).getInstance(TestResource.class);
    }

    @Test
    public void scopeSingleton() {
        sut.register(Scope.SINGLETON, TestResource.class);
        startAndWait(sut);
        get("/");
        get("/");
        assertEquals(2, test.counter);
        verify(injector, times(1)).getInstance(TestResource.class);
    }

    @Test
    public void scopePrototype() {
        sut.register(Scope.PROTOTYPE, TestResource.class);
        startAndWait(sut);
        get("/");
        get("/");
        assertEquals(1, test.counter);
        verify(injector, times(2)).getInstance(TestResource.class);
    }

    @Test
    public void objectMapperIsUsed() throws JsonProcessingException {
        sut.register(Scope.PROTOTYPE, TestResource.class);
        startAndWait(sut);
        var actual = get("/json");
        var expected = "123";
        assertEquals(expected, actual);
    }

    private static void startAndWait(HttpServerImpl sut) {
        sut.start();
        while (sut.isStarting()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String get(String path) {
        var res = ClientBuilder.newClient(new ClientConfig()) //
                .target(SERVER_URL) //
                .path(path) //
                .request(MediaType.APPLICATION_JSON) //
                .accept(MediaType.APPLICATION_JSON) //
                .get(Response.class);
        return res.readEntity(String.class);
    }

    @Path("/")
    public static class TestResource {

        public int counter = 0;

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String get() {
            counter++;
            return "\"ok\"";
        }

        @GET
        @Path("/json")
        @Produces(MediaType.APPLICATION_JSON)
        public TestData getData() {
            return new TestData();
        }

    }

    public static class TestData {
        public int x = 123;
    }

    private static class TestModule extends SimpleModule {
        private static final long serialVersionUID = 1L;

        private TestModule() {
            addSerializer(TestData.class, new JsonSerializer<TestData>() {
                @Override
                public void serialize(TestData t, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    gen.writeNumber(t.x);
                }
            });
            addDeserializer(TestData.class, new JsonDeserializer<TestData>() {
                @Override
                public TestData deserialize(JsonParser p, DeserializationContext ctxt)
                        throws IOException, JacksonException {
                    var t = new TestData();
                    t.x = p.getIntValue();
                    return t;
                }
            });
        }
    }
}