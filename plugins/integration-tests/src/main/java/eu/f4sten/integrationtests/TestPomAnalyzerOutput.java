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
package eu.f4sten.integrationtests;

import static eu.f4sten.infra.kafka.DefaultTopics.POM_ANALYZER;
import static eu.f4sten.infra.kafka.Lane.ERROR;
import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static eu.f4sten.infra.kafka.Lane.PRIORITY;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.kafka.Message;
import eu.f4sten.integrationtests.utils.Messages;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.maven.data.PomAnalysisResult;

public class TestPomAnalyzerOutput implements Plugin {

    private final Messages msgs;

    @Inject
    public TestPomAnalyzerOutput(Messages msgs) {
        this.msgs = msgs;
    }

    @Override
    public void run() {
        msgs.collectAll();

        System.out.println("### ERRORS ###\n");
        var errs = msgs.get(POM_ANALYZER, ERROR, new TRef<Message<MavenId, Void>>() {});

        errs.stream() //
                .collect(Collectors.groupingBy(m -> m.error.type)) //
                .forEach((type, msgs) -> {
                    System.out.printf("%5dx %s\n", msgs.size(), type);
                });
        System.out.println();

        errs.forEach(m -> {
            System.out.printf("##\n## %s\n##\n", toCoord(m.input), m.error.type);
            System.out.printf("\nError type: %s\n", m.error.type);
            var msg = m.error.stacktrace.trim();// .replace("\n", "\n| ");
            System.out.printf("\n%s\n\n", msg);
        });

        var counter = new int[] { 0 };
        System.out.println("### NORMAL ###");
        msgs.get(POM_ANALYZER, NORMAL, new TRef<Message<Void, PomAnalysisResult>>() {}).stream() //
                .sorted(new Comparator<Message<Void, PomAnalysisResult>>() {
                    @Override
                    public int compare(Message<Void, PomAnalysisResult> a, Message<Void, PomAnalysisResult> b) {
                        return toCoord(a.payload).compareTo(toCoord(b.payload));
                    }
                }).forEach(m -> {
                    System.out.printf("%d) %s\n", counter[0]++, toCoord(m.payload));
                });

        System.out.println("### PRIO ###");

        counter[0] = 0;
        msgs.get(POM_ANALYZER, PRIORITY, new TRef<Message<Void, PomAnalysisResult>>() {}).stream() //
                .sorted(new Comparator<Message<Void, PomAnalysisResult>>() {
                    @Override
                    public int compare(Message<Void, PomAnalysisResult> a, Message<Void, PomAnalysisResult> b) {
                        return toCoord(a.payload).compareTo(toCoord(b.payload));
                    }
                }).forEach(m -> {
                    System.out.printf("%d) %s\n", counter[0]++, toCoord(m.payload));
                });
    }

    private String toCoord(PomAnalysisResult res) {
        return String.format("%s:%s:%s:%s (%s)", res.groupId, res.artifactId, res.packagingType, res.version,
                res.artifactRepository);
    }

    private String toCoord(MavenId id) {
        return String.format("%s:%s:?:%s (%s)", id.groupId, id.artifactId, id.version, id.artifactRepository);
    }
}