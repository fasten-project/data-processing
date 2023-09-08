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
package eu.f4sten.pomanalyzer.utils;

import static eu.fasten.core.data.metadatadb.codegen.tables.ArtifactRepositories.ARTIFACT_REPOSITORIES;
import static eu.fasten.core.data.metadatadb.codegen.tables.Dependencies.DEPENDENCIES;
import static eu.fasten.core.data.metadatadb.codegen.tables.IngestedArtifacts.INGESTED_ARTIFACTS;
import static eu.fasten.core.data.metadatadb.codegen.tables.IngestionRetries.INGESTION_RETRIES;
import static eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions.PACKAGE_VERSIONS;
import static eu.fasten.core.data.metadatadb.codegen.tables.Packages.PACKAGES;

import java.sql.Timestamp;
import java.util.Date;

import org.jooq.DSLContext;
import org.jooq.JSONB;

import com.github.t9t.jooq.json.JsonbDSL;

import eu.fasten.core.data.metadatadb.codegen.Keys;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;

public class MetadataDao {

    private DSLContext context;

    public MetadataDao(DSLContext context) {
        this.context = context;
    }

    public DSLContext getContext() {
        return this.context;
    }

    public void setContext(DSLContext context) {
        this.context = context;
    }

    public long insertPackage(String packageName, String forge, String projectName, String repository, Timestamp createdAt) {
        var resultRecord = context.insertInto(PACKAGES, //
                PACKAGES.PACKAGE_NAME, //
                PACKAGES.FORGE, //
                PACKAGES.PROJECT_NAME, //
                PACKAGES.REPOSITORY, //
                PACKAGES.CREATED_AT) //
                .values(packageName, forge, projectName, repository, createdAt) //
                .onConflictOnConstraint(Keys.UNIQUE_PACKAGE_FORGE).doUpdate().set(PACKAGES.PROJECT_NAME, PACKAGES.as("excluded").PROJECT_NAME) //
                .set(PACKAGES.REPOSITORY, PACKAGES.as("excluded").REPOSITORY).set(PACKAGES.CREATED_AT, PACKAGES.as("excluded").CREATED_AT) //
                .returning(Packages.PACKAGES.ID).fetchOne();
        return resultRecord.getValue(Packages.PACKAGES.ID);
    }

    public long insertPackage(String packageName, String forge) {
        var resultRecord = context.insertInto(PACKAGES, //
                PACKAGES.PACKAGE_NAME, //
                PACKAGES.FORGE) //
                .values(packageName, forge) //
                .onConflictOnConstraint(Keys.UNIQUE_PACKAGE_FORGE) //
                .doUpdate() //
                .set(PACKAGES.PACKAGE_NAME, PACKAGES.as("excluded").PACKAGE_NAME).set(PACKAGES.FORGE, PACKAGES.as("excluded").FORGE) //
                .returning(PACKAGES.ID) //
                .fetchOne();
        return resultRecord.getValue(Packages.PACKAGES.ID);
    }

    public long insertArtifactRepository(String repositoryBaseUrl) {
        var result = context.insertInto(ARTIFACT_REPOSITORIES, //
                ARTIFACT_REPOSITORIES.REPOSITORY_BASE_URL) //
                .values(repositoryBaseUrl) //
                .onConflictOnConstraint(Keys.UNIQUE_ARTIFACT_REPOSITORIES) //
                .doUpdate().set(ARTIFACT_REPOSITORIES.REPOSITORY_BASE_URL, ARTIFACT_REPOSITORIES.as("excluded").REPOSITORY_BASE_URL) //
                .returning(ARTIFACT_REPOSITORIES.ID) //
                .fetchOne();
        if (result == null) {
            return -1;
        }
        return result.getId();
    }

    public long insertPackageVersion(long packageId, String cgGenerator, String version, Long artifactRepositoryId, String architecture, Timestamp createdAt, String metadata) {
        var metadataJsonb = metadata != null ? JSONB.valueOf(metadata) : null;
        var resultRecord = context.insertInto(PACKAGE_VERSIONS, //
                PACKAGE_VERSIONS.PACKAGE_ID, //
                PACKAGE_VERSIONS.CG_GENERATOR, //
                PACKAGE_VERSIONS.VERSION, //
                PACKAGE_VERSIONS.ARTIFACT_REPOSITORY_ID, //
                PACKAGE_VERSIONS.ARCHITECTURE, //
                PACKAGE_VERSIONS.CREATED_AT, //
                PACKAGE_VERSIONS.METADATA).values(packageId, cgGenerator, version, artifactRepositoryId, architecture, createdAt, metadataJsonb)
                .onConflictOnConstraint(Keys.UNIQUE_PACKAGE_VERSION_GENERATOR) //
                .doUpdate() //
                .set(PACKAGE_VERSIONS.CREATED_AT, PACKAGE_VERSIONS.as("excluded").CREATED_AT) //
                .set(PACKAGE_VERSIONS.METADATA, JsonbDSL.concat( //
                        PACKAGE_VERSIONS.METADATA, //
                        PACKAGE_VERSIONS.as("excluded").METADATA))
                .returning(PackageVersions.PACKAGE_VERSIONS.ID).fetchOne();
        return resultRecord.getValue(PackageVersions.PACKAGE_VERSIONS.ID);
    }

    public void insertIngestedArtifact(String key, String pluginVersion) {
        var timestamp = new Timestamp(new Date().getTime());
        context.insertInto(INGESTED_ARTIFACTS, //
                INGESTED_ARTIFACTS.KEY, //
                INGESTED_ARTIFACTS.PLUGIN_VERSION, //
                INGESTED_ARTIFACTS.TIMESTAMP).values(key, pluginVersion, timestamp).execute();
    }

    public long insertDependency(long packageVersionId, long dependencyId, String[] versionRanges, String[] architecture, String[] dependencyType, Long alternativeGroup, String metadata) {
        var resultRecord = context.insertInto(DEPENDENCIES, //
                DEPENDENCIES.PACKAGE_VERSION_ID, //
                DEPENDENCIES.DEPENDENCY_ID, //
                DEPENDENCIES.VERSION_RANGE, //
                DEPENDENCIES.ARCHITECTURE, //
                DEPENDENCIES.DEPENDENCY_TYPE, //
                DEPENDENCIES.ALTERNATIVE_GROUP, //
                DEPENDENCIES.METADATA) //
                .values(packageVersionId, dependencyId, versionRanges, architecture, dependencyType, alternativeGroup, JSONB.valueOf(metadata))
                .onConflictOnConstraint(Keys.UNIQUE_VERSION_DEPENDENCY_RANGE) //
                .doUpdate() //
                .set(DEPENDENCIES.VERSION_RANGE, DEPENDENCIES.as("excluded").VERSION_RANGE) //
                .set(DEPENDENCIES.ARCHITECTURE, DEPENDENCIES.as("excluded").ARCHITECTURE) //
                .set(DEPENDENCIES.DEPENDENCY_TYPE, DEPENDENCIES.as("excluded").DEPENDENCY_TYPE) //
                .set(DEPENDENCIES.ALTERNATIVE_GROUP, DEPENDENCIES.as("excluded").ALTERNATIVE_GROUP) //
                .set(DEPENDENCIES.METADATA, JsonbDSL.concat( //
                        DEPENDENCIES.METADATA, //
                        DEPENDENCIES.as("excluded").METADATA))
                .returning(DEPENDENCIES.PACKAGE_VERSION_ID).fetchOne();
        return resultRecord.getValue(DEPENDENCIES.PACKAGE_VERSION_ID);
    }

    public int getIngestionRetryCount(String key) {
        var res = context.select(INGESTION_RETRIES.COUNT) //
                .from(INGESTION_RETRIES) //
                .where(INGESTION_RETRIES.KEY.eq(key)) //
                .fetchOne();
        if (res == null) {
            return 0;
        } else {
            return res.value1();
        }
    }

    public void registerIngestionRetry(String key) {
        int count = getIngestionRetryCount(key);
        if (count == 0) {
            context.insertInto(INGESTION_RETRIES, INGESTION_RETRIES.KEY, INGESTION_RETRIES.COUNT) //
                    .values(key, Short.valueOf("1")) //
                    .execute();
        } else {
            var nextCount = Short.valueOf(String.valueOf(count + 1));
            context.update(INGESTION_RETRIES) //
                    .set(INGESTION_RETRIES.COUNT, nextCount) //
                    .where(INGESTION_RETRIES.KEY.eq(key)) //
                    .execute();
        }
    }

    public void pruneIngestionRetries(String key) {
        context.deleteFrom(INGESTION_RETRIES) //
                .where(INGESTION_RETRIES.KEY.eq(key)) //
                .execute();
    }

    public boolean isArtifactIngested(String key) {
        var result = context.select(INGESTED_ARTIFACTS.TIMESTAMP) //
                .from(INGESTED_ARTIFACTS) //
                .where(INGESTED_ARTIFACTS.KEY.eq(key)) //
                .fetch();
        return !result.isEmpty();
    }
}