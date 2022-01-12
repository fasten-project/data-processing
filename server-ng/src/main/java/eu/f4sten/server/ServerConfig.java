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

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;

import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.utils.HostName;
import eu.f4sten.server.core.utils.IoUtils;
import eu.f4sten.server.core.utils.JsonUtils;
import eu.f4sten.server.core.utils.Kafka;
import eu.f4sten.server.core.utils.PostgresConnector;
import eu.f4sten.server.utils.HostNameImpl;
import eu.f4sten.server.utils.IoUtilsImpl;
import eu.f4sten.server.utils.JsonUtilsImpl;
import eu.f4sten.server.utils.KafkaConnector;
import eu.f4sten.server.utils.KafkaImpl;
import eu.f4sten.server.utils.PostgresConnectorImpl;

public class ServerConfig implements IInjectorConfig {

	private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

	private Args args;

	public ServerConfig(Args args) {
		this.args = args;
	}

	@Override
	public void configure(Binder binder) {
		binder.bind(Args.class).toInstance(args);
		binder.bind(HostName.class).to(HostNameImpl.class);
		binder.bind(Kafka.class).to(KafkaImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	public IoUtils bindIoUtils(JsonUtils jsonUtils) {
		assertFor(args) //
				.notNull(args -> args.baseDir, "base dir") //
				.that(args -> args.baseDir.exists(), "base dir does not exist");
		return new IoUtilsImpl(args.baseDir, jsonUtils);
	}

	@Provides
	public KafkaConnector bindKafkaConnector(HostName hostName) {
		assertFor(args) //
				.notNull(args -> args.kafkaUrl, "kafka url") //
				.that(args -> args.instanceId == null || (args.instanceId != null && !args.instanceId.isEmpty()),
						"instance id must be null or non-empty");

		return new KafkaConnector(hostName, args.kafkaUrl, args.plugin);
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
		return new JacksonModule();
	}

	@Provides
	@Singleton
	public ObjectMapper bindObjectMapper(Set<Module> modules) {
		LOG.info("Instantiating ObjectMapper from {} modules: {}", modules.size(), modules);

		return JsonMapper.builder() //
				.disable(MapperFeature.AUTO_DETECT_GETTERS) // do not create json fields for getters
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
//				.enable(SerializationFeature.INDENT_OUTPUT) // pretty printing
				.build() //
				.setVisibility(PropertyAccessor.ALL, Visibility.ANY) //
				.registerModules(modules);
	}
}