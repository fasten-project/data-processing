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
package eu.f4sten.depgraph;

import com.google.inject.Binder;
import com.google.inject.Provides;

import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;
import eu.fasten.core.maven.resolution.IMavenResolver;
import eu.fasten.core.maven.resolution.MavenDependencyData;
import eu.fasten.core.maven.resolution.MavenDependencyResolver;
import eu.fasten.core.maven.resolution.MavenDependentsData;
import eu.fasten.core.maven.resolution.MavenDependentsResolver;
import eu.fasten.core.maven.resolution.MavenResolver;

@InjectorConfig
public class DepGraphConfig implements IInjectorConfig {

    private DepGraphArgs args;

    public DepGraphConfig(DepGraphArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DepGraphArgs.class).toInstance(args);
        binder.bind(IMavenResolver.class).to(MavenResolver.class);
        binder.bind(MavenDependencyData.class).toInstance(new MavenDependencyData());
        binder.bind(MavenDependentsData.class).toInstance(new MavenDependentsData());
    }

    @Provides
    public MavenDependencyResolver provideDependencyResolver(MavenDependencyData data) {
        var r = new MavenDependencyResolver();
        r.setData(data);
        return r;
    }

    @Provides
    public MavenDependentsResolver provideDependentsResolver(MavenDependentsData data) {
        var r = new MavenDependentsResolver();
        r.setData(data);
        return r;
    }
}