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
package eu.f4sten.infra.impl.kafka;

import static eu.f4sten.test.TestLoggerUtils.assertLogsContain;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.test.TestLoggerUtils;

public class KafkaGracefulShutdownThreadTest {

    private Kafka kafka;
    private KafkaGracefulShutdownThread sut;

    @BeforeEach
    public void setup() {
        TestLoggerUtils.clearLog();
        kafka = mock(Kafka.class);
        sut = new KafkaGracefulShutdownThread(kafka);
    }

    @Test
    public void whenExecutedShutdownKafka() {
        sut.run();
        verify(kafka).stop();
    }

    @Test
    public void shutdownIsLogged() {
        sut.run();
        assertLogsContain(KafkaGracefulShutdownThread.class, "INFO Gracefully stopping all Kafka connections ...");
    }
}