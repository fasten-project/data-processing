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

import static eu.fasten.core.maven.utils.MavenUtilities.MAVEN_CENTRAL_REPO;

import java.sql.Timestamp;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.utils.Version;
import eu.fasten.core.data.Constants;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.maven.data.PomAnalysisResult;

public class DatabaseUtils {

    private final DSLContext context;
    private final JsonUtils jsonUtils;
    private final Version version;

    private boolean processedRecord;

    public DatabaseUtils(DSLContext context, JsonUtils jsonUtils, Version version) {
        this.context = context;
        this.jsonUtils = jsonUtils;
        this.version = version;
    }

    protected MetadataDao getDao(DSLContext ctx) {
        return new MetadataDao(ctx);
    }

    public void save(PomAnalysisResult result) {
        processedRecord = false;
        int numTries = 0;
        while (!processedRecord && numTries < Constants.transactionRestartLimit) {
            numTries++;
            context.transaction(transaction -> {
                var dao = getDao(DSL.using(transaction));
                try {
                    insertIntoDB(result, dao);
                    processedRecord = true;
                } catch (DataAccessException e) {
                    // Can be happen. Normally fixable through retrying.
                }
            });
        }
    }

    public void insertIntoDB(PomAnalysisResult r, MetadataDao dao) {
        String product = r.groupId + Constants.mvnCoordinateSeparator + r.artifactId;
        final var packageId = dao.insertPackage(product, Constants.mvnForge, r.projectName, r.repoUrl, null);

        var pvMeta = jsonUtils.toJson(r);

        var isMavenCentral = MAVEN_CENTRAL_REPO.equals(r.artifactRepository);
        long artifactRepoId = isMavenCentral ? -1L : dao.insertArtifactRepository(r.artifactRepository);

        // TODO: Why is the opalGenerator required here??
        final var packageVersionId = dao.insertPackageVersion(packageId, Constants.opalGenerator, r.version,
                artifactRepoId, null, getProperTimestamp(r.releaseDate), pvMeta);

        for (var dep : r.dependencies) {
            var depProduct = dep.groupId + Constants.mvnCoordinateSeparator + dep.artifactId;
            final var depId = dao.insertPackage(depProduct, Constants.mvnForge);
            var json = jsonUtils.toJson(dep);
            dao.insertDependency(packageVersionId, depId, dep.getVersionConstraints(), null, null, null, json);
        }
    }

    public void markAsIngestedPackage(String gapv, Lane lane) {
        if (!hasPackageBeenIngested(gapv, lane)) {
            var dao = getDao(context);
            dao.insertIngestedArtifact(toKey(gapv, lane), version.get());
        }
    }

    private static String toKey(String gapv, Lane lane) {
        return String.format("%s-%s", gapv, lane);
    }

    public boolean hasPackageBeenIngested(String gapv, Lane lane) {
        var key = toKey(gapv, lane);
        var dao = getDao(context);
        return dao.isArtifactIngested(key);
    }

    private static Timestamp getProperTimestamp(long timestamp) {
        if (timestamp == -1) {
            return null;
        } else {
            // TODO get rid of this code if it does not appear in the log
            if (timestamp / (1000L * 60 * 60 * 24 * 365) < 1L) {
                // return new Timestamp(timestamp * 1000);
                throw new RuntimeException(
                        "this should be a relict of the past, fix DatabaseUtils.getProperTimestamp, if this error appears in the log");
            }
            return new Timestamp(timestamp);
        }
    }

    public int getRetryCount(String key) {
        return getDao(context).getIngestionRetryCount(key);
    }

    public void registerRetry(String key) {
        getDao(context).registerIngestionRetry(key);
    }

    public void pruneRetries(String key) {
        getDao(context).pruneIngestionRetries(key);
    }
}