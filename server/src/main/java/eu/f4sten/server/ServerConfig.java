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

import static eu.f4sten.server.core.AssertArgs.assertFor;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;

import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.InjectorConfig;
import eu.f4sten.server.core.json.JsonUtils;
import eu.f4sten.server.core.json.ObjectMapperBuilder;
import eu.f4sten.server.core.kafka.Kafka;
import eu.f4sten.server.core.kafka.MessageGenerator;
import eu.f4sten.server.core.utils.HostName;
import eu.f4sten.server.core.utils.IoUtils;
import eu.f4sten.server.core.utils.PostgresConnector;
import eu.f4sten.server.core.utils.Version;
import eu.f4sten.server.json.JsonUtilsImpl;
import eu.f4sten.server.kafka.KafkaImpl;
import eu.f4sten.server.kafka.MessageGeneratorImpl;
import eu.f4sten.server.utils.HostNameImpl;
import eu.f4sten.server.utils.IoUtilsImpl;
import eu.f4sten.server.utils.PostgresConnectorImpl;
import eu.f4sten.server.utils.VersionImpl;

@InjectorConfig
public class ServerConfig implements IInjectorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

    private final ServerArgs args;

    public ServerConfig(ServerArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ServerArgs.class).toInstance(args);
        binder.bind(HostName.class).to(HostNameImpl.class);
        binder.bind(Kafka.class).to(KafkaImpl.class).in(Scopes.SINGLETON);
        binder.bind(Version.class).to(VersionImpl.class);
        binder.bind(MessageGenerator.class).to(MessageGeneratorImpl.class);
    }

    @Provides
    public IoUtils bindIoUtils(JsonUtils jsonUtils) {
        assertFor(args) //
                .notNull(args -> args.baseDir, "base dir") //
                .that(args -> args.baseDir.exists(), "base dir does not exist");
        return new IoUtilsImpl(args.baseDir, jsonUtils);
    }

    @Provides
    public PostgresConnector bindPostgresConnector() {
        assertFor(args) //
                .notNull(a -> a.dbUrl, "db url") //
                .notNull(a -> a.dbUser, "db user") //
                .that(a -> !a.dbUrl.contains("@"), "providing user via db url is not supported") //
                .that(a -> a.dbUrl.startsWith("jdbc:postgresql://"), "db url does not start with 'jdbc:postgresql://'");

        return new PostgresConnectorImpl(args.dbUrl, args.dbUser, true);
    }

    @Provides
    @Singleton
    public JsonUtils bindJsonUtils(ObjectMapper om) {
        return new JsonUtilsImpl(om);
    }

    @ProvidesIntoSet
    public Module bindJacksonModule() {
        return new SimpleModule();
    }

    @Provides
    @Singleton
    public ObjectMapper bindObjectMapper(Set<Module> modules) {
        LOG.info("Instantiating ObjectMapper from {} modules: {}", modules.size(), modules);

        return new ObjectMapperBuilder().build() //
                .registerModules(modules);
    }
}