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
import java.io.IOException;
import java.io.InputStreamReader;

import javax.inject.Inject;

import eu.f4sten.another.data.TestData;
import eu.f4sten.server.core.Plugin;
import eu.f4sten.server.core.kafka.Kafka;
import eu.f4sten.server.core.kafka.Lane;

public class Another implements Plugin {

    private final Kafka kafka;
    private final MyArgs args;

    @Inject
    public Another(Kafka kafka, MyArgs args) {
        this.kafka = kafka;
        this.args = args;
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
            kafka.publish(t, args.kafkaOut, Lane.PRIORITY);
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