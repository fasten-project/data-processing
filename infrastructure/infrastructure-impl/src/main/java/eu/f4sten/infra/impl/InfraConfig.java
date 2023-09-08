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
package eu.f4sten.infra.impl;

import static dev.c0ps.diapper.AssertArgs.assertFor;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;

import dev.c0ps.diapper.IInjectorConfig;
import dev.c0ps.diapper.InjectorConfig;
import dev.c0ps.diapper.RunnerArgs;
import dev.c0ps.franz.Kafka;
import dev.c0ps.franz.KafkaConnector;
import dev.c0ps.franz.KafkaGracefulShutdownThread;
import dev.c0ps.franz.KafkaImpl;
import dev.c0ps.io.IoUtils;
import dev.c0ps.io.IoUtilsImpl;
import dev.c0ps.io.JsonUtils;
import dev.c0ps.io.JsonUtilsImpl;
import dev.c0ps.io.ObjectMapperBuilder;
import dev.c0ps.libhttpd.HttpServer;
import dev.c0ps.libhttpd.HttpServerGracefulShutdownThread;
import dev.c0ps.libhttpd.HttpServerImpl;
import dev.c0ps.maven.json.CommonsMavenDataModule;
import eu.f4sten.infra.impl.kafka.MessageGeneratorImpl;
import eu.f4sten.infra.impl.utils.HostNameImpl;
import eu.f4sten.infra.impl.utils.PostgresConnectorImpl;
import eu.f4sten.infra.impl.utils.VersionImpl;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.infra.utils.HostName;
import eu.f4sten.infra.utils.PostgresConnector;
import eu.f4sten.infra.utils.Version;

@InjectorConfig
public class InfraConfig implements IInjectorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(InfraConfig.class);

    private final InfraArgs args;

    public InfraConfig(InfraArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(InfraArgs.class).toInstance(args);
        binder.bind(HostName.class).to(HostNameImpl.class);
        binder.bind(Version.class).to(VersionImpl.class);
        binder.bind(MessageGenerator.class).to(MessageGeneratorImpl.class);
    }

    @Provides
    public HttpServer bindHttpServer(Injector injector) {
        var server = new HttpServerImpl(injector, args.httpPort, args.httpBaseUrl);
        Runtime.getRuntime().addShutdownHook(new HttpServerGracefulShutdownThread(server));
        return server;
    }

    @Provides
    public IoUtils bindIoUtils(JsonUtils jsonUtils, ObjectMapper om) {
        assertFor(args) //
                .notNull(args -> args.baseDir, "base dir") //
                .that(args -> args.baseDir.exists(), "base dir does not exist");
        return new IoUtilsImpl(args.baseDir, jsonUtils, om);
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
    public KafkaConnector bindKafkaConnector(RunnerArgs runnerArgs) {
        assertFor(args) //
                .notNull(a -> a.kafkaUrl, "kafka url") //
                .that(a -> a.instanceId == null || !a.instanceId.isEmpty(), "instance id must be null or non-empty") //
                .that(a -> a.kafkaGroupId == null || !a.kafkaGroupId.isEmpty(), "group id must be null or non-empty");

        var pluginId = runnerArgs.run.replace("eu.f4sten.", "");
        pluginId = pluginId.endsWith(".Main") //
                ? pluginId.replace(".Main", "") //
                : pluginId;

        var serverUrl = args.kafkaUrl;
        var groupId = args.kafkaGroupId != null ? args.kafkaGroupId : pluginId;
        var instanceId = args.instanceId;
        return new KafkaConnector(serverUrl, groupId, instanceId);
    }

    @Provides
    @Singleton
    public Kafka bindKafka(JsonUtils jsonUtils, KafkaConnector connector) {
        var kafka = new KafkaImpl(jsonUtils, connector, args.kafkaShouldAutoCommit);
        Runtime.getRuntime().addShutdownHook(new KafkaGracefulShutdownThread(kafka));
        return kafka;
    }

    @Provides
    @Singleton
    public JsonUtils bindJsonUtils(ObjectMapper om) {
        return new JsonUtilsImpl(om);
    }

    @ProvidesIntoSet
    public Module provideCoreMavenDataModule() {
        return new CommonsMavenDataModule();
    }

    @Provides
    @Singleton
    public ObjectMapper bindObjectMapper(Set<Module> modules) {
        LOG.info("Instantiating ObjectMapper from {} modules: {}", modules.size(), modules);

        return new ObjectMapperBuilder().build() //
                .registerModules(modules);
    }
}