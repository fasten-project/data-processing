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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Binder;
import com.google.inject.multibindings.ProvidesIntoSet;

import eu.f4sten.server.core.InjectorConfig;
import eu.f4sten.server.core.IInjectorConfig;

@InjectorConfig
public class MyConfig implements IInjectorConfig {

	private MyArgs myArgs;

	public MyConfig(MyArgs myArgs) {
		this.myArgs = myArgs;
	}

	@Override
	public void configure(Binder binder) {
		binder.bind(MyArgs.class).toInstance(myArgs);
	}

	@ProvidesIntoSet
	public Module bindJacksonModule() {
		return new SimpleModule();
	}
}