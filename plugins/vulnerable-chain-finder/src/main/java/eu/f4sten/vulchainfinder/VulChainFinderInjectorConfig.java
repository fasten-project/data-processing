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
package eu.f4sten.vulchainfinder;

import com.fasterxml.jackson.databind.Module;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;
import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.utils.PostgresConnector;
import eu.f4sten.vulchainfinder.json.FastenURIJacksonModule;
import eu.f4sten.vulchainfinder.utils.DatabaseUtils;
import eu.f4sten.vulchainfinder.utils.RestAPIDependencyResolver;
import eu.fasten.core.data.callableindex.RocksDao;
import eu.fasten.core.vulchains.VulnerableCallChainRepository;
import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.rocksdb.RocksDBException;

@InjectorConfig
public class VulChainFinderInjectorConfig implements IInjectorConfig {

    private VulChainFinderArgs args;

    public VulChainFinderInjectorConfig(VulChainFinderArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(VulChainFinderArgs.class).toInstance(args);
    }

    @Provides
    public DatabaseUtils bindDatabaseUtils(PostgresConnector pc, JsonUtils json) {
        var c = pc.getNewConnection();
        var dslContext = DSL.using(c, SQLDialect.POSTGRES);
        return new DatabaseUtils(dslContext, json);
    }

    @Provides
    @Singleton
    public RocksDao bindRocksDao() throws RocksDBException {
        return new RocksDao(args.callableIndexPath, false);
    }

    @Provides
    public RestAPIDependencyResolver bindRestAPIDependencyResolver(){
        return new RestAPIDependencyResolver(args.restApiBaseURL, HttpClient.newBuilder().build());
    }

    @Provides
    public VulnerableCallChainRepository bindVulnerableCallChainRepository(){
        try {
            return new VulnerableCallChainRepository(args.restApiBaseURL);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @ProvidesIntoSet
    public Module bindJacksonModule() {
        return new FastenURIJacksonModule();
    }
}