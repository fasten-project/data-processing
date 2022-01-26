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
package eu.f4sten.mavencrawler;

import static java.util.concurrent.TimeUnit.HOURS;

import java.util.concurrent.Executors;

import javax.inject.Inject;

import eu.f4sten.infra.Plugin;
import eu.f4sten.mavencrawler.utils.IndexProcessor;

public class Main implements Plugin {

    private final IndexProcessor processor;

    @Inject
    public Main(IndexProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void run() {
        processor.tryProcessingNextIndices();
        scheduleHourly(() -> {
            processor.tryProcessingNextIndices();
        });
    }

    private void scheduleHourly(Runnable r) {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(r, 0, 1, HOURS);
    }
}