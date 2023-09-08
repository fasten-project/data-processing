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
package eu.f4sten.pomanalyzer.utils;

import static dev.c0ps.franz.Lane.NORMAL;
import static eu.f4sten.infra.utils.FastenConstants.FORGE_MVN;
import static eu.f4sten.infra.utils.FastenConstants.OPAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Date;

import org.jooq.DSLContext;
import org.jooq.TransactionalRunnable;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dev.c0ps.io.JsonUtils;
import dev.c0ps.maven.MavenUtilities;
import dev.c0ps.maven.data.Dependency;
import dev.c0ps.maven.data.PomBuilder;
import eu.f4sten.infra.exceptions.UnrecoverableError;
import eu.f4sten.infra.utils.Version;

public class DatabaseUtilsTest {

    private static final String SOME_KEY = "abc";
    private static final DataAccessException DAE = mock(DataAccessException.class);
    private static final String SOME_PLUGIN_VERSION = "0.1.2";
    private MetadataDao dao;
    private JsonUtils json;
    private DSLContext dslContext;
    private Version version;

    private DatabaseUtils sut;

    @BeforeEach
    public void setup() {
        dao = mock(MetadataDao.class);
        json = mock(JsonUtils.class);
        dslContext = mock(DSLContext.class);
        version = mock(Version.class);
        when(version.get()).thenReturn(SOME_PLUGIN_VERSION);

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock i) throws Throwable {
                var arg0 = (TransactionalRunnable) i.getArgument(0);
                arg0.run(null);
                return null;
            }
        }).when(dslContext).transaction(any(TransactionalRunnable.class));

        sut = new DatabaseUtils(dslContext, json, version) {
            protected MetadataDao getDao(DSLContext ctx) {
                return dao;
            }
        };
    }

    @Test
    public void transactionIsStarted() {
        var result = getSomeResult();
        sut.save(result.pom());
        verify(dslContext).transaction(any(TransactionalRunnable.class));

    }

    @Test
    public void storePackage() {
        var result = getSomeResult();
        result.artifactRepository = MavenUtilities.MAVEN_CENTRAL_REPO;
        sut.save(result.pom());
        verify(dao).insertPackage("g:a", FORGE_MVN, result.projectName, result.repoUrl, null);
    }

    @Test
    public void doNotStoreMavenCentralRepo() {
        var result = getSomeResult();
        result.artifactRepository = MavenUtilities.MAVEN_CENTRAL_REPO;
        sut.save(result.pom());
        verify(dao, times(0)).insertArtifactRepository(anyString());
    }

    @Test
    public void storeNonMavenCentralRepo() {
        var result = getSomeResult();
        result.artifactRepository = "Non Maven Central";
        sut.save(result.pom());
        verify(dao).insertArtifactRepository(result.artifactRepository);
    }

    @Test
    public void storePackageVersion() {
        var result = getSomeResult();
        when(json.toJson(eq(result.pom()))).thenReturn("<some json>");

        when(dao.insertPackage(anyString(), anyString(), anyString(), anyString(), eq(null))).thenReturn(123L);
        when(dao.insertArtifactRepository(anyString())).thenReturn(234L);
        sut.save(result.pom());

        var captor = ArgumentCaptor.forClass(String.class);

        verify(dao).insertPackageVersion(eq(123L), eq(OPAL), eq(result.version), eq(234L), eq(null), eq(new Timestamp(result.releaseDate)), captor.capture());

        var actualJson = captor.getValue();
        var expectedJson = "<some json>";
        assertEquals(expectedJson.toString(), actualJson.toString());
    }

    @Test
    public void storeDependency() {
        var result = getSomeResult();

        when(json.toJson(eq(result.pom()))).thenReturn("<some json>");
        when(json.toJson(eq(result.dependencies.iterator().next()))).thenReturn("<some dep json>");

        when(dao.insertPackage(anyString(), anyString(), anyString(), anyString(), eq(null))).thenReturn(123L);
        when(dao.insertPackage(anyString(), anyString())).thenReturn(123L);
        when(dao.insertPackageVersion(anyLong(), anyString(), anyString(), anyLong(), eq(null), any(Timestamp.class), any(String.class))).thenReturn(234L);

        sut.save(result.pom());

        var arrCaptor = ArgumentCaptor.forClass(String[].class);
        var jsonCaptor = ArgumentCaptor.forClass(String.class);

        verify(dao).insertPackage("dg1:da1", FORGE_MVN);
        verify(dao).insertDependency(eq(234L), eq(123L), arrCaptor.capture(), eq(null), eq(null), eq(null), jsonCaptor.capture());

        var actual = arrCaptor.getValue();
        assertEquals(1, actual.length);
        assertEquals("dv1", actual[0]);

        var actualJson = jsonCaptor.getValue();
        var expectedJson = "<some dep json>";
        assertEquals(expectedJson.toString(), actualJson.toString());
    }

    @Test
    public void insertIngest() {
        sut.markAsIngestedPackage("gapv", NORMAL);
        verify(dao).insertIngestedArtifact("gapv-NORMAL", SOME_PLUGIN_VERSION);
    }

    @Test
    public void hasIngested() {
        sut.hasPackageBeenIngested("gapv", NORMAL);
        verify(dao).isArtifactIngested("gapv-NORMAL");
    }

    @Test
    public void registerRetry() {
        sut.registerRetry(SOME_KEY);
        verify(dao).registerIngestionRetry(SOME_KEY);
    }

    @Test
    public void getRetryCount() {
        when(dao.getIngestionRetryCount(SOME_KEY)).thenReturn(1234);
        var actual = sut.getRetryCount(SOME_KEY);
        var expected = 1234;
        assertEquals(expected, actual);
    }

    @Test
    public void pruneRetries() {
        sut.pruneRetries(SOME_KEY);
        verify(dao).pruneIngestionRetries(SOME_KEY);
    }

    @Test
    public void assertDBExceptionIsHandled_save() {
        doThrow(DAE).when(dslContext).transaction(any(TransactionalRunnable.class));
        assertUnrecoverableError(DAE, () -> {
            sut.save(getSomeResult().pom());
        });
    }

    @Test
    public void assertDBExceptionIsHandled_insertIntoDB() {
        doThrow(DAE).when(dao).insertPackage(anyString(), anyString(), anyString(), anyString(), eq(null));
        assertUnrecoverableError(DAE, () -> {
            sut.save(getSomeResult().pom());
        });
    }

    @Test
    public void assertDBExceptionIsHandled_markAsIngestedPackage() {
        doThrow(DAE).when(dao).insertIngestedArtifact(anyString(), anyString());
        assertUnrecoverableError(DAE, () -> {
            sut.markAsIngestedPackage("g:a:jar:1", NORMAL);
        });
    }

    @Test
    public void assertDBExceptionIsHandled_hasPackageBeenIngested() {
        doThrow(DAE).when(dao).isArtifactIngested(anyString());
        assertUnrecoverableError(DAE, () -> {
            sut.hasPackageBeenIngested("...", NORMAL);
        });
    }

    @Test
    public void assertDBExceptionIsHandled_getRetryCount() {
        doThrow(DAE).when(dao).getIngestionRetryCount(anyString());
        assertUnrecoverableError(DAE, () -> {
            sut.getRetryCount("...");
        });
    }

    @Test
    public void assertDBExceptionIsHandled_registerRetry() {
        doThrow(DAE).when(dao).registerIngestionRetry(anyString());
        assertUnrecoverableError(DAE, () -> {
            sut.registerRetry("...");
        });
    }

    @Test
    public void assertDBExceptionIsHandled_pruneRetries() {
        doThrow(DAE).when(dao).pruneIngestionRetries(anyString());
        assertUnrecoverableError(DAE, () -> {
            sut.pruneRetries("...");
        });
    }

    private static void assertUnrecoverableError(Throwable cause, Executable r) {
        var e = assertThrows(UnrecoverableError.class, r);
        Throwable actual = e.getCause();
        assertSame(cause, actual);
    }

    private static PomBuilder getSomeResult() {
        var result = new PomBuilder();
        result.artifactRepository = "...";
        result.groupId = "g";
        result.artifactId = "a";
        result.packagingType = "jar";
        result.version = "1.2.3";

        result.projectName = "projectName";
        result.repoUrl = "repoUrl";
        result.commitTag = "commitTag";
        result.dependencies.add(new Dependency("dg1", "da1", "dv1"));
        result.dependencyManagement.add(new Dependency("dmg1", "dma1", "dmv1"));

        result.releaseDate = new Date().getTime();
        result.parentCoordinate = "pg:pa:pom:2.3.4";
        result.sourcesUrl = "soruceUrl";
        return result;
    }
}