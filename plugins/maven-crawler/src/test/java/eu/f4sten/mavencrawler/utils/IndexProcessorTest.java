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
package eu.f4sten.mavencrawler.utils;

import static dev.c0ps.franz.Lane.NORMAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.c0ps.franz.Kafka;
import dev.c0ps.maveneasyindex.Artifact;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;

public class IndexProcessorTest {

    private static final Artifact SOME_MAVEN_ID = mock(Artifact.class);
    private static final String SOME_TOPIC = "abcd";

    private MavenCrawlerArgs args;
    private LocalStore store;
    private EasyIndexClient utils;
    private Kafka kafka;

    private IndexProcessor sut;

    @BeforeEach
    public void setup() {
        args = new MavenCrawlerArgs();
        args.kafkaOut = SOME_TOPIC;
        store = mock(LocalStore.class);
        utils = mock(EasyIndexClient.class);
        kafka = mock(Kafka.class);
        sut = new IndexProcessor(args, store, utils, kafka);
    }

    @Test
    public void correctDataFlowInMethod() {

        when(store.getNextIndex()).thenReturn(123);
        when(utils.exists(123)).thenReturn(true);
        when(utils.get(123)).thenReturn(List.of(SOME_MAVEN_ID));

        sut.tryProcessingNextIndices();

        // first iteration
        verify(store).getNextIndex();
        verify(utils).exists(123);
        verify(utils).get(123);
        verify(kafka).publish(SOME_MAVEN_ID, SOME_TOPIC, NORMAL);
        // second iteration
        verify(utils).exists(124);
    }
}