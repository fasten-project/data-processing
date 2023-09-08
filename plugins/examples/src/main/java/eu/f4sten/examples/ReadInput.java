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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.franz.Kafka;
import dev.c0ps.franz.Lane;
import eu.f4sten.examples.data.SomeInputData;
import jakarta.inject.Inject;

public class ReadInput implements Runnable {

    private final Kafka kafka;
    private final MyArgs args;

    @Inject
    public ReadInput(Kafka kafka, MyArgs args) {
        this.kafka = kafka;
        this.args = args;
        AssertArgs.notNull(args, a -> a.kafkaOut, "kafka out");
    }

    @Override
    public void run() {
        System.out.printf("Reading from terminal, publishing to %s ...\n", args.kafkaOut);

        var isRunning = true;
        while (isRunning) {

            var input = readNextLine();
            if (input.isEmpty()) {
                System.out.print("No input provided, aborting.");
                isRunning = false;
                continue;
            }
            var t = new SomeInputData(input, new Date());
            kafka.publish(t, args.kafkaOut, Lane.PRIORITY);
        }
    }

    private String readNextLine() {
        System.out.print("Please provide a name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}