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

import dev.c0ps.diapper.AssertArgs;
import dev.c0ps.franz.Kafka;
import eu.f4sten.examples.data.SomeInputData;
import jakarta.inject.Inject;

public class TransformInput implements Runnable {

    private final Kafka kafka;
    private final MyArgs args;

    @Inject
    public TransformInput(Kafka kafka, MyArgs args) {
        this.kafka = kafka;
        this.args = args;
        AssertArgs.assertFor(args) //
                .notNull(a -> a.kafkaIn, "kafka in") //
                .notNull(a -> a.kafkaOut, "kafka out");
    }

    @Override
    public void run() {
        System.out.printf("Subscribing to %s, publishing to %s ...\n", args.kafkaIn, args.kafkaOut);

        kafka.subscribe(args.kafkaIn, SomeInputData.class, (in, l) -> {
            System.out.printf("Found message: %s\n", in);

            var out = in.input.toUpperCase();

            System.out.printf("Publishing transformation: %s\n", out);
            kafka.publish(out, args.kafkaOut, l);
        });

        while (true) {
            kafka.poll();
        }
    }
}