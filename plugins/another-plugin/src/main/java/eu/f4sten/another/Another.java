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
package eu.f4sten.another;

import static eu.f4sten.server.core.AssertArgs.assertFor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.another.data.TestData;
import eu.f4sten.server.core.Plugin;
import eu.f4sten.server.core.utils.IoUtils;
import eu.f4sten.server.core.utils.Kafka;
import eu.f4sten.server.core.utils.Lane;

public class Another implements Plugin {
	
	private static final Logger LOG = LoggerFactory.getLogger(Another.class);

	private final Kafka kafka;
	private final AnotherArgs args;

	private IoUtils io;

	@Inject
	public Another(Kafka kafka, AnotherArgs args, IoUtils io) {
		this.kafka = kafka;
		this.args = args;
		this.io = io;
	}

	@Override
	public void run() {
		assertFor(args) //
				.notNull(a -> a.kafkaOut, "kafka output topic");

		var isRunning = true;
		while (isRunning) {

			var name = readNextName();
			if (name.isEmpty()) {
				System.out.print("No name provided, aborting.");
				isRunning = false;
				continue;
			}
			var t = new TestData(name, 3);
			kafka.publish(t, args.kafkaOut, Lane.NORMAL);
		}
	}

	private String readNextName() {
		System.out.print("Please provide a name: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			return br.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}