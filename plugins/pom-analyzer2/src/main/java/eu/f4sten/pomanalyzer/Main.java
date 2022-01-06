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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.another.data.TestData;
import eu.f4sten.server.core.AssertArgs;
import eu.f4sten.server.core.Plugin;
import eu.f4sten.server.core.utils.Kafka;
import eu.f4sten.server.core.utils.Lane;

public class Main implements Plugin {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private final MyArgs args;
	private final Kafka kafka;

	@Inject
	public Main(MyArgs args, Kafka kafka) {
		this.args = args;
		this.kafka = kafka;
	}

	@Override
	public void run() {
		AssertArgs.assertFor(args) //
				.notNull(a -> a.kafkaIn, "input topic") //
				.notNull(a -> a.kafkaOut, "output topic");

		kafka.subscribe(args.kafkaIn, TestData.class, this::process);
		while (true) {
			kafka.poll();
		}
	}

	private void process(TestData td, Lane l) {

		LOG.info("Found data on lane {}: {}", l, td);

		var output = new Output(td.name);
		output.prev = td;

		kafka.publish(output, args.kafkaOut, l);
	}
}