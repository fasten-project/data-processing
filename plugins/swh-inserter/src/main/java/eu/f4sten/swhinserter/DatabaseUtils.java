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
package eu.f4sten.swhinserter;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.json.JSONObject;

import eu.fasten.core.data.metadatadb.codegen.tables.Files;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import eu.fasten.core.exceptions.UnrecoverableError;

public class DatabaseUtils {

    private final DSLContext context;

    public DatabaseUtils(DSLContext context) {
        this.context = context;
    }

    public Long getPkgVersionID(String pkgName, String version) {
        try {
            var pkgVerID = context.select(PackageVersions.PACKAGE_VERSIONS.ID)
                    .from(Packages.PACKAGES, PackageVersions.PACKAGE_VERSIONS)
                    .where(Packages.PACKAGES.PACKAGE_NAME.eq(pkgName)
                            .and(PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID.eq(Packages.PACKAGES.ID))
                            .and(PackageVersions.PACKAGE_VERSIONS.VERSION.eq(version)))
                    .fetchOne();
            // May produce null pointer exception
            return pkgVerID.component1();
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public List<String> getFilePaths4PkgVersion(Long pkgVersionID) {
        try {
            var filePaths = context.select(Files.FILES.PATH).from(Files.FILES)
                    .where(Files.FILES.PACKAGE_VERSION_ID.eq(pkgVersionID)).fetch();
            return filePaths.getValues(Files.FILES.PATH);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public String addFileHash(Long pkgVersionID, String filePath, String fileHash) {
        try {
            final var fileHashFieldName = "swh_checksum";
            final var currentMetadata = context.select(Files.FILES.METADATA).from(Files.FILES)
                    .where(Files.FILES.PACKAGE_VERSION_ID.eq(pkgVersionID)
                            .and(Files.FILES.PATH.eq(filePath))).fetchOne().getValue(Files.FILES.METADATA);

            // Updates the existing metadata field with file hash to avoid overriding it
            var fileMetadata = currentMetadata == null
                    ? new JSONObject().put(fileHashFieldName, fileHash)
                    : new JSONObject(currentMetadata.data()).put(fileHashFieldName, fileHash);

            return context.update(Files.FILES).set(Files.FILES.METADATA, JSONB.valueOf(String.valueOf(fileMetadata)))
                    .where(Files.FILES.PACKAGE_VERSION_ID.eq(pkgVersionID).and(Files.FILES.PATH.eq(filePath)))
                    .returningResult(Files.FILES.PATH).fetchOne().getValue(Files.FILES.PATH);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }
}