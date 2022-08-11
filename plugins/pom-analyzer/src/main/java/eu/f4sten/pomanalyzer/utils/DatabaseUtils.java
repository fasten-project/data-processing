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
import java.util.List;

import eu.fasten.core.data.metadatadb.codegen.tables.Files;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.utils.Version;
import eu.fasten.core.data.Constants;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.exceptions.UnrecoverableError;
import eu.fasten.core.maven.data.Pom;
import org.json.JSONObject;

public class DatabaseUtils {

    private final DSLContext context;
    private final JsonUtils jsonUtils;
    private final Version version;

    public DatabaseUtils(DSLContext context, JsonUtils jsonUtils, Version version) {
        this.context = context;
        this.jsonUtils = jsonUtils;
        this.version = version;
    }

    protected MetadataDao getDao(DSLContext ctx) {
        return new MetadataDao(ctx);
    }

    public void save(Pom result) {
        try {
            context.transaction(transaction -> {
                var dao = getDao(DSL.using(transaction));
                insertIntoDB(result, dao);
            });
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    @SuppressWarnings("deprecation")
    private void insertIntoDB(Pom r, MetadataDao dao) {
        var product = r.groupId + Constants.mvnCoordinateSeparator + r.artifactId;
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
            dao.insertDependency(packageVersionId, depId, dep.getVersionConstraintsArr(), null, null, null, json);
        }
    }

    public void markAsIngestedPackage(String gapv, Lane lane) {
        try {
            if (!hasPackageBeenIngested(gapv, lane)) {
                var dao = getDao(context);
                dao.insertIngestedArtifact(toKey(gapv, lane), version.get());
            }
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    private static String toKey(String gapv, Lane lane) {
        return String.format("%s-%s", gapv, lane);
    }

    public boolean hasPackageBeenIngested(String gapv, Lane lane) {
        try {
            var key = toKey(gapv, lane);
            var dao = getDao(context);
            return dao.isArtifactIngested(key);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
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
        try {
            return getDao(context).getIngestionRetryCount(key);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public void registerRetry(String key) {
        try {
            getDao(context).registerIngestionRetry(key);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public void pruneRetries(String key) {
        try {
            getDao(context).pruneIngestionRetries(key);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public Long getPkgVersionID(String pkgName, String version) {
        try {
            var pkgVerID = context.select(PackageVersions.PACKAGE_VERSIONS.ID).
                    from(Packages.PACKAGES, PackageVersions.PACKAGE_VERSIONS).
                    where(Packages.PACKAGES.PACKAGE_NAME.eq(pkgName).
                                    and(PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID.eq(Packages.PACKAGES.ID)).
                                    and(PackageVersions.PACKAGE_VERSIONS.VERSION.eq(version))).fetchOne();
            // May produce null pointer exception
            return pkgVerID.component1();
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public List<String> getFilePaths4PkgVersion(Long pkgVersionID) {
        try {
            var filePaths = context.select(Files.FILES.PATH).
                    from(Files.FILES).where(Files.FILES.PACKAGE_VERSION_ID.eq(pkgVersionID)).fetch();
            return filePaths.getValues(Files.FILES.PATH);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public String addFileHash(Long pkgVersionID, String filePath, String fileHash) {
        try {
            var fileMetadata = JSONB.valueOf(String.valueOf(new JSONObject().put("swh_checksum", fileHash)));
            return context.update(Files.FILES).
                    set(Files.FILES.METADATA, fileMetadata).
                    where(Files.FILES.PACKAGE_VERSION_ID.eq(pkgVersionID).and(Files.FILES.PATH.eq(filePath))).
                    returningResult(Files.FILES.PATH).fetchOne().getValue(Files.FILES.PATH);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }
}