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
package eu.f4sten.examples;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Binder;
import com.google.inject.multibindings.ProvidesIntoSet;

import eu.f4sten.examples.data.SomeInputData;
import eu.f4sten.examples.data.SomeInputDataJson;
import eu.f4sten.server.core.IInjectorConfig;
import eu.f4sten.server.core.InjectorConfig;

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

    @ProvidesIntoSet
    public Module provideJacksonModule() {
        var m = new SimpleModule();
        m.addSerializer(SomeInputData.class, new SomeInputDataJson.SomeInputDataSerializer());
        m.addDeserializer(SomeInputData.class, new SomeInputDataJson.SomeInputDataDeserializer());
        return m;
    }
}