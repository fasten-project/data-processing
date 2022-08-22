/*
 * Copyright 2022 Delft University of Technology
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
package eu.f4sten.depgraph;

import static eu.f4sten.test.TestLoggerUtils.assertLogsContain;
import static eu.f4sten.test.TestLoggerUtils.clearLog;
import static eu.fasten.core.maven.data.Scope.PROVIDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import eu.f4sten.test.TestLoggerUtils;
import eu.fasten.core.maven.data.ResolvedRevision;
import eu.fasten.core.maven.data.Scope;
import eu.fasten.core.maven.resolution.IMavenResolver;
import eu.fasten.core.maven.resolution.MavenResolutionException;
import eu.fasten.core.maven.resolution.ResolverConfig;

public class DependencyGraphResolutionTest {

    private static final Set<String> SOME_DEPS = Set.of("a:a:1", "b:b:2");
    private static final Set<ResolvedRevision> SOME_RESULTS = Set.of(rr(1), rr(2));
    private static final ResolverConfig DEFAULT_CFG = new ResolverConfig();

    private DependencyGraphResolution sut;
    private IMavenResolver resolver;
    private ResolverConfig lastCfg;

    @BeforeEach
    public void setup() {
        clearLog();
        DEFAULT_CFG.resolveAt = new Date().getTime();
        lastCfg = null;
        resolver = mock(IMavenResolver.class);
        sut = new DependencyGraphResolution(resolver);

        var cfgCaptor = ArgumentCaptor.forClass(ResolverConfig.class);
        var answerWithSomeResults = new Answer<Set<ResolvedRevision>>() {
            @Override
            public Set<ResolvedRevision> answer(InvocationOnMock invocation) throws Throwable {
                lastCfg = cfgCaptor.getValue();
                return SOME_RESULTS;
            }
        };
        when(resolver.resolveDependencies(anySet(), cfgCaptor.capture())).thenAnswer(answerWithSomeResults);
        when(resolver.resolveDependents(anyString(), anyString(), anyString(), cfgCaptor.capture()))
                .thenAnswer(answerWithSomeResults);
    }

    @Test
    public void deps_defaultRequest() {
        var res = sut.resolveDependencies(SOME_DEPS, null, null, null, null, null, null);
        assertResolveAt();
        verify(resolver).resolveDependencies(eq(SOME_DEPS), eq(DEFAULT_CFG));

        assertEquals(HttpStatus.SC_OK, res.getStatus());
        assertEquals(SOME_RESULTS, res.getEntity());
    }

    @Test
    public void deps_resolutionException() {

        when(resolver.resolveDependencies(anySet(), any(ResolverConfig.class)))
                .thenThrow(new MavenResolutionException("<mvn>"));

        var res = sut.resolveDependencies(Set.of("g1:a1:1.1.1"), null, null, null, null, null, null);
        var status = res.getStatusInfo();
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, status.getStatusCode());
        assertEquals("<mvn>", status.getReasonPhrase());

        var logs = TestLoggerUtils.getFormattedLogs(DependencyGraphResolution.class);
        assertEquals(1, logs.size());
        var log = String.format("ERROR MavenResolutionException in resolveDependencies([g1:a1:1.1.1], %s",
                ResolverConfig.class.getName());
        assertTrue(logs.get(0).startsWith(log));
    }

    @Test
    public void deps_generalException() {

        when(resolver.resolveDependencies(anySet(), any(ResolverConfig.class)))
                .thenThrow(new RuntimeException("<general>"));

        var res = sut.resolveDependencies(Set.of("g1:a1:1.1.1"), null, null, null, null, null, null);
        var status = res.getStatusInfo();
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, status.getStatusCode());
        assertEquals("<general>", status.getReasonPhrase());

        var logs = TestLoggerUtils.getFormattedLogs(DependencyGraphResolution.class);
        assertEquals(1, logs.size());
        var log = String.format("ERROR RuntimeException in resolveDependencies([g1:a1:1.1.1], %s",
                ResolverConfig.class.getName());
        assertTrue(logs.get(0).startsWith(log));
    }

    @Test
    public void dpts_defaultRequest() {
        var actual = sut.resolveDependents("g1", "a1", "1.1.1", null, null, null, null, null, null);
        assertResolveAt();
        verify(resolver).resolveDependents(eq("g1"), eq("a1"), eq("1.1.1"), eq(DEFAULT_CFG));

        assertEquals(HttpStatus.SC_OK, actual.getStatus());
        assertEquals(SOME_RESULTS, actual.getEntity());
    }

    @Test
    public void dpts_resolutionException() {

        when(resolver.resolveDependents(anyString(), anyString(), anyString(), any(ResolverConfig.class)))
                .thenThrow(new MavenResolutionException("<mvn>"));

        var res = sut.resolveDependents("g1", "a1", "1.1.1", null, null, null, null, null, null);
        var status = res.getStatusInfo();
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, status.getStatusCode());
        assertEquals("<mvn>", status.getReasonPhrase());

        var logs = TestLoggerUtils.getFormattedLogs(DependencyGraphResolution.class);
        assertEquals(1, logs.size());
        var log = String.format("ERROR MavenResolutionException in resolveDependents([g1:a1:1.1.1], %s",
                ResolverConfig.class.getName());
        assertTrue(logs.get(0).startsWith(log));
    }

    @Test
    public void dpts_generalException() {

        when(resolver.resolveDependents(anyString(), anyString(), anyString(), any(ResolverConfig.class)))
                .thenThrow(new RuntimeException("<general>"));

        var res = sut.resolveDependents("g1", "a1", "1.1.1", null, null, null, null, null, null);
        var status = res.getStatusInfo();
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, status.getStatusCode());
        assertEquals("<general>", status.getReasonPhrase());

        var logs = TestLoggerUtils.getFormattedLogs(DependencyGraphResolution.class);
        assertEquals(1, logs.size());
        var log = String.format("ERROR RuntimeException in resolveDependents([g1:a1:1.1.1], %s",
                ResolverConfig.class.getName());
        assertTrue(logs.get(0).startsWith(log));
    }

    @Test
    public void cfgResolveAt() {
        testDepsAndDpts(1234L, null, null, null, null, null, () -> {
            assertEquals(1234L, lastCfg.resolveAt);
            lastCfg.resolveAt = DEFAULT_CFG.resolveAt;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgDepthMax() {
        testDepsAndDpts(null, "mAx", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(Integer.MAX_VALUE, lastCfg.depth);
            lastCfg.depth = DEFAULT_CFG.depth;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgDepthTrans() {
        testDepsAndDpts(null, "TrAnSitive", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(Integer.MAX_VALUE, lastCfg.depth);
            lastCfg.depth = DEFAULT_CFG.depth;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgDepthDirect() {
        testDepsAndDpts(null, "dIrEcT", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(1, lastCfg.depth);
            lastCfg.depth = DEFAULT_CFG.depth;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgDepthNumber() {
        testDepsAndDpts(null, "234", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(234, lastCfg.depth);
            lastCfg.depth = DEFAULT_CFG.depth;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgDepthInvalid() {
        testDepsAndDpts(null, "-1", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(DEFAULT_CFG, lastCfg);
            assertLogsContain(DependencyGraphResolution.class, "ERROR Ignoring invalid depth (-1)");
        });
    }

    @Test
    public void cfgDepthUnparseable() {
        testDepsAndDpts(null, "X", null, null, null, null, () -> {
            assertResolveAt();
            assertEquals(DEFAULT_CFG, lastCfg);
            assertLogsContain(DependencyGraphResolution.class, "ERROR Ignoring unparseable depth (X)");
        });
    }

    @Test
    public void cfgLimit() {
        testDepsAndDpts(null, null, 132, null, null, null, () -> {
            assertResolveAt();
            assertEquals(132, lastCfg.limit);
            lastCfg.limit = DEFAULT_CFG.limit;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgLimitNegative() {
        testDepsAndDpts(null, null, -1, null, null, null, () -> {
            assertResolveAt();
            assertEquals(DEFAULT_CFG, lastCfg);
            assertLogsContain(DependencyGraphResolution.class, "ERROR Ignoring invalid limit (-1)");
        });
    }

    @Test
    public void cfgScope() {
        testDepsAndDpts(null, null, null, PROVIDED, null, null, () -> {
            assertResolveAt();
            assertEquals(PROVIDED, lastCfg.scope);
            lastCfg.scope = DEFAULT_CFG.scope;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgInclProvided() {
        testDepsAndDpts(null, null, null, null, true, null, () -> {
            assertResolveAt();
            assertTrue(lastCfg.alwaysIncludeProvided);
            lastCfg.alwaysIncludeProvided = DEFAULT_CFG.alwaysIncludeProvided;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    @Test
    public void cfgInclOpt() {
        testDepsAndDpts(null, null, null, null, null, true, () -> {
            assertResolveAt();
            assertTrue(lastCfg.alwaysIncludeOptional);
            lastCfg.alwaysIncludeOptional = DEFAULT_CFG.alwaysIncludeOptional;
            assertEquals(DEFAULT_CFG, lastCfg);
        });
    }

    private void testDepsAndDpts(Long resolveAt, String depth, Integer limit, Scope scope,
            Boolean alwaysIncludeProvided, Boolean alwaysIncludeOptional, Runnable tests) {

        sut.resolveDependents("g1", "a1", "1.1.1", resolveAt, depth, limit, scope, alwaysIncludeProvided,
                alwaysIncludeOptional);
        tests.run();

        setup();
        sut.resolveDependencies(Set.of("g1:a1:1.1.1"), resolveAt, depth, limit, scope, alwaysIncludeProvided,
                alwaysIncludeOptional);
        tests.run();
    }

    private void assertResolveAt() {
        assertNotNull(lastCfg);
        assertTrue(lastCfg.resolveAt - DEFAULT_CFG.resolveAt < 1000);
        lastCfg.resolveAt = DEFAULT_CFG.resolveAt;
    }

    private static ResolvedRevision rr(int id) {
        return new ResolvedRevision(id, "g" + id, "a" + id, id + ".0.0", new Timestamp(id), Scope.COMPILE);
    }
}