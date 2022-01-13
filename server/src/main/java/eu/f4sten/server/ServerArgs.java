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

import java.io.File;

import com.beust.jcommander.Parameter;

public class ServerArgs {

	@Parameter(names = "--plugin", arity = 1, required = true, description = "fully-qualified class name of the plugin to be started")
	public String plugin;

	@Parameter(names = "--db.url", arity = 1, description = "JDBC url for the database")
	public String dbUrl;

	@Parameter(names = "--db.user", arity = 1, description = "user for the database connection")
	public String dbUser;

	@Parameter(names = "--logLevel", arity = 1, description = "Desired log level")
	public LogLevel logLevel = LogLevel.INFO;

	@Parameter(names = "--baseDir", arity = 1, description = "Base folder for all file-based operations")
	public File baseDir;

	@Parameter(names = "--kafka.url", arity = 1, description = "address for the Kafka Server")
	public String kafkaUrl;

	@Parameter(names = "--instanceId", arity = 1, description = "uniquely identifies this instance of the application")
	public String instanceId = null;

	@Parameter // keep to prevent failures when CLI command has main arg
	private String mainArg;
}
