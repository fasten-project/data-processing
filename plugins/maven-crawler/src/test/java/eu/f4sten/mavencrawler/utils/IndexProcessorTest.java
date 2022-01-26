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

import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;
import eu.f4sten.pomanalyzer.data.MavenId;

public class IndexProcessorTest {

    private static final File SOME_FILE = mock(File.class);
    private static final MavenId SOME_MAVEN_ID = mock(MavenId.class);
    private static final String SOME_TOPIC = "abcd";

    private MavenCrawlerArgs args;
    private LocalStore store;
    private RemoteUtils utils;
    private FileReader reader;
    private Kafka kafka;

    private IndexProcessor sut;

    @BeforeEach
    public void setup() {
        args = new MavenCrawlerArgs();
        args.kafkaOut = SOME_TOPIC;
        store = mock(LocalStore.class);
        utils = mock(RemoteUtils.class);
        reader = mock(FileReader.class);
        kafka = mock(Kafka.class);
        sut = new IndexProcessor(args, store, utils, reader, kafka);
    }

    @Test
    public void correctDataFlowInMethod() {

        when(store.getNextIndex()).thenReturn(123);
        when(utils.exists(123)).thenReturn(true);
        when(utils.download(123)).thenReturn(SOME_FILE);
        when(reader.readIndexFile(SOME_FILE)).thenReturn(Set.of(SOME_MAVEN_ID));

        sut.tryProcessingNextIndices();

        // first iteration
        verify(store).getNextIndex();
        verify(utils).exists(123);
        verify(utils).download(123);
        verify(reader).readIndexFile(SOME_FILE);
        verify(kafka).publish(SOME_MAVEN_ID, SOME_TOPIC, NORMAL);
        // second iteration
        verify(utils).exists(124);
    }
}