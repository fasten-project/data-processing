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
package eu.f4sten.pomanalyzer;

import static eu.f4sten.infra.kafka.Lane.PRIORITY;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.fasterxml.jackson.databind.Module;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;

import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;
import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.utils.PostgresConnector;
import eu.f4sten.infra.utils.Version;
import eu.f4sten.pomanalyzer.json.CoreJacksonModule;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import eu.f4sten.pomanalyzer.utils.Resolver;

@InjectorConfig
public class MyInjectorConfig implements IInjectorConfig {

    private MyArgs args;

    public MyInjectorConfig(MyArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(MyArgs.class).toInstance(args);
    }

    @Provides
    public DatabaseUtils bindDatabaseUtils(PostgresConnector pc, JsonUtils json, Version version) {
        var c = pc.getNewConnection();
        var dslContext = DSL.using(c, SQLDialect.POSTGRES);
        return new DatabaseUtils(dslContext, json, version);
    }

    @Provides
    public Resolver bindResolver(DatabaseUtils dbUtils) {
        return new Resolver(gapv -> dbUtils.hasPackageBeenIngested(gapv, PRIORITY));
    }

    @ProvidesIntoSet
    public Module bindJacksonModule() {
        return new CoreJacksonModule();
    }
}