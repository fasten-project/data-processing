/*
 * Copyright 2022 Software Improvement Group
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
package eu.f4sten.sourcesprovider;

import com.google.inject.Binder;

import dev.c0ps.diapper.IInjectorConfig;
import dev.c0ps.diapper.InjectorConfig;

@InjectorConfig
public class SourcesProviderInjectorConfig implements IInjectorConfig {

    private SourcesProviderArgs args;

    public SourcesProviderInjectorConfig(SourcesProviderArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(SourcesProviderArgs.class).toInstance(args);
    }
}