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

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.google.inject.Binder;
import com.google.inject.Provides;

import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;
import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.utils.PostgresConnector;
import eu.f4sten.infra.utils.Version;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;

@InjectorConfig
public class PomAnalyzerInjectorConfig implements IInjectorConfig {

    private PomAnalyzerArgs args;

    public PomAnalyzerInjectorConfig(PomAnalyzerArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(PomAnalyzerArgs.class).toInstance(args);
    }

    @Provides
    public DatabaseUtils bindDatabaseUtils(PostgresConnector pc, JsonUtils json, Version version) {
        var c = pc.getNewConnection();
        var dslContext = DSL.using(c, SQLDialect.POSTGRES);
        return new DatabaseUtils(dslContext, json, version);
    }
}