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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.MessageGenerator;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import eu.f4sten.pomanalyzer.utils.EffectiveModelBuilder;
import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;
import eu.f4sten.pomanalyzer.utils.PackagingFixer;
import eu.f4sten.pomanalyzer.utils.PomExtractor;
import eu.f4sten.pomanalyzer.utils.ProgressTracker;
import eu.f4sten.pomanalyzer.utils.Resolver;
import eu.fasten.core.maven.data.PomAnalysisResult;

public class MainTest {

    private MavenRepositoryUtils repo;
    private EffectiveModelBuilder modelBuilder;
    private PomExtractor extractor;
    private DatabaseUtils db;
    private Resolver resolver;
    private Kafka kafka;
    private PomAnalyzerArgs args;
    private MessageGenerator msgs;
    private PackagingFixer fixer;

    private Main sut;
    private ProgressTracker tracker;

    @BeforeEach
    public void setup() {
        tracker = mock(ProgressTracker.class);
        repo = mock(MavenRepositoryUtils.class);
        modelBuilder = mock(EffectiveModelBuilder.class);
        extractor = mock(PomExtractor.class);
        db = mock(DatabaseUtils.class);
        resolver = mock(Resolver.class);
        kafka = mock(Kafka.class);
        args = new PomAnalyzerArgs();
        msgs = mock(MessageGenerator.class);
        fixer = mock(PackagingFixer.class);

        sut = new Main(tracker, repo, modelBuilder, extractor, db, resolver, kafka, args, msgs, fixer);

        when(extractor.process(eq(null))).thenReturn(new PomAnalysisResult());
        when(extractor.process(any(Model.class))).thenReturn(new PomAnalysisResult());
        when(fixer.checkPackage(any(PomAnalysisResult.class))).thenReturn("jar");
    }

    @Test
    public void basicSmokeTest() {
        sut.hashCode();
        // sut.consume("{\"groupId\":\"log4j\",\"artifactId\":\"log4j\",\"version\":\"1.2.17\"}",
        // NORMAL);
    }

    // TODO extend test suite, right now this is only a stub for easy debugging
}