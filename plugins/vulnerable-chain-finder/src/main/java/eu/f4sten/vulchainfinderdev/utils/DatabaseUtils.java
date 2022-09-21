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

package eu.f4sten.vulchainfinderdev.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.DirectedGraph;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.data.metadatadb.codegen.tables.Callables;
import eu.fasten.core.data.metadatadb.codegen.tables.Modules;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import eu.fasten.core.data.metadatadb.codegen.tables.Vulnerabilities;
import eu.fasten.core.data.metadatadb.codegen.tables.VulnerabilitiesXCallables;
import eu.fasten.core.data.metadatadb.codegen.tables.VulnerabilitiesXPackageVersions;
import eu.fasten.core.data.vulnerability.Vulnerability;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.fasten.core.exceptions.UnrecoverableError;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

public class DatabaseUtils {

    private final DSLContext context;
    private final JsonUtils jsonUtils;

    public DatabaseUtils(DSLContext context, JsonUtils jsonUtils) {
        this.context = context;
        this.jsonUtils = jsonUtils;
    }

    public DSLContext getContext() {
        return context;
    }

    public MetadataDao getDao(DSLContext ctx) {
        return new MetadataDao(ctx);
    }

    public Map<FastenURI, List<Vulnerability>> selectVulCallablesOf(final Set<Long> depIds) {
        Map<FastenURI, List<Vulnerability>> vulCallables = new HashMap<>();
        for (Long depId : depIds) {
            vulCallables = selectVulCallablesOf(depId);
        }
        return vulCallables;
    }

    /*
    This method avoids looping over pkg. version IDs and uses only one DB query. Hence it is expected to be faster
     to retrieve vulnerable callables.
     */
    public ConcurrentHashMap<FastenURI, List<Vulnerability>> selectVulnerableCallables(final Set<Long> vulnDepIds) {
        ConcurrentHashMap<FastenURI, List<Vulnerability>> vulCallables = new ConcurrentHashMap<>();

        // Tables
        Vulnerabilities v = Vulnerabilities.VULNERABILITIES;
        VulnerabilitiesXPackageVersions vxp = VulnerabilitiesXPackageVersions.VULNERABILITIES_X_PACKAGE_VERSIONS;
        VulnerabilitiesXCallables vxc = VulnerabilitiesXCallables.VULNERABILITIES_X_CALLABLES;
        PackageVersions pv = PackageVersions.PACKAGE_VERSIONS;
        Packages p = Packages.PACKAGES;
        Callables c = Callables.CALLABLES;

        try {
            context.select(p.PACKAGE_NAME, pv.VERSION, c.FASTEN_URI, v.STATEMENT, v.EXTERNAL_ID)
                    .from(v, vxp, vxc, p, pv, c)
                    .where(vxp.PACKAGE_VERSION_ID.in(vulnDepIds))
                    .and(vxp.PACKAGE_VERSION_ID.eq(pv.ID))
                    .and(pv.PACKAGE_ID.eq(p.ID))
                    .and(v.ID.eq(vxp.VULNERABILITY_ID))
                    .and(v.ID.eq(vxc.VULNERABILITY_ID))
                    .and(c.ID.eq(vxc.CALLABLE_ID)).fetch().parallelStream().forEach(r -> {
                        final var vulMap = convertRecordToVulMap(Objects.requireNonNull(r.get(4)).toString(),
                                r.get(3));
                        if (!vulMap.isEmpty()) {
                            vulCallables.put(createFastenUriFromPckgVersionUriFields(r),
                                    new ArrayList<>(vulMap.values()));
                        }
                    });
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
        return vulCallables;
    }

    public Map<FastenURI, List<Vulnerability>> selectVulCallablesOf(final long depId) {

        final var moduleIds = selectAllModulesOf(depId);
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return selectConcurrentlyVulCallablesOf(moduleIds);
    }

    public Map<FastenURI, List<Vulnerability>> selectConcurrentlyVulCallablesOf(
        final Set<Long> moduleIds) {
        Map<FastenURI, List<Vulnerability>> result = new ConcurrentHashMap<>();
        moduleIds.parallelStream().forEach(moduleId ->
            context.fetch(createStrForSelectVulCallablesWhereModuleIdIs(moduleId))
                .forEach(record -> {
                    final var vulMap = convertRecordToVulMap(record);
                    if (!vulMap.isEmpty()) {
                        result.put(createFastenUriFromPckgVersionUriFields(record),
                            new ArrayList<>(vulMap.values()));
                    }
                }));
        return result;
    }

    private Map<String, Vulnerability> convertRecordToVulMap(final Record record) {
        final var vulField = record.get(3);
        if (vulField == null) {
            return Collections.emptyMap();
        }
        final var setType = new TRef<HashMap<String, Vulnerability>>() {};
        return jsonUtils.fromJson(vulField.toString(), setType);
    }

    private Map<String, Vulnerability> convertRecordToVulMap(final String vulnId, final Object vulnObject) {
        if (vulnObject == null) {
            return Collections.emptyMap();
        }
        final var vulnType = new TRef<Vulnerability>() {};
        final var vulMap = new HashMap<String, Vulnerability>();
        vulMap.put(vulnId, jsonUtils.fromJson(vulnObject.toString(), vulnType));
        return vulMap;
    }

    public static FastenURI createFastenUriFromPckgVersionUriFields(final Record record) {
        final var uriString =
            String.format("%s%s$%s%s",
                "fasten://mvn!", record.get(0), record.get(1), record.get(2));
        return FastenURI.create(uriString);
    }

    public static String createStrForSelectVulCallablesWhereModuleIdIs(final Long moduleId) {
        return "SELECT packages.package_name, package_versions.version, callables.fasten_uri, " +
            "callables.metadata -> 'vulnerabilities' " +
            "FROM callables, modules, package_versions, packages " +
            "where modules.id = callables.module_id " +
            "and package_versions.id = modules.package_version_id " +
            "and packages.id = package_versions.package_id " +
            "and callables.metadata -> 'vulnerabilities' IS NOT NULL " +
            "and callables.module_id = " + moduleId;
    }

    public Set<Long> selectAllModulesOf(final long depId) {
        return context.select(Modules.MODULES.ID).from(Modules.MODULES)
            .where(Modules.MODULES.PACKAGE_VERSION_ID.eq(depId)).fetch()
            .intoSet(Modules.MODULES.ID);
    }

    public Set<Long> selectVulnerablePackagesExistingIn(final Set<Long> depIds) {
        try {
            return getDao(context).findVulnerablePackageVersions(depIds);
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    public Long getPackageVersionID(final MavenId mvnId) {
        try {
            return context.select(PackageVersions.PACKAGE_VERSIONS.ID)
                    .from(Packages.PACKAGES, PackageVersions.PACKAGE_VERSIONS)
                    .where(Packages.PACKAGES.ID.eq(PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID))
                    .and(Packages.PACKAGES.PACKAGE_NAME.eq(mvnId.getProductName()))
                    .and(PackageVersions.PACKAGE_VERSIONS.VERSION.eq(mvnId.getProductVersion())).fetchOne().component1();
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
    }

    /**
     * Gets all the FASTEN URIs of a given directed graph from the metadata DB
     */
    public BiMap<Long, String> getAllUrisFromDB(DirectedGraph dg){
        Set<Long> gIDs = new HashSet<>();
        for (Long node : dg.nodes()) {
            if (node > 0) {
                gIDs.add(node);
            }
        }
        BiMap<Long, String> uris = HashBiMap.create();
        try {
            context.select(Callables.CALLABLES.ID, Packages.PACKAGES.PACKAGE_NAME,
                            PackageVersions.PACKAGE_VERSIONS.VERSION,
                            Callables.CALLABLES.FASTEN_URI)
                    .from(Callables.CALLABLES, Modules.MODULES, PackageVersions.PACKAGE_VERSIONS, Packages.PACKAGES)
                    .where(Callables.CALLABLES.ID.in(gIDs))
                    .and(Modules.MODULES.ID.eq(Callables.CALLABLES.MODULE_ID))
                    .and(PackageVersions.PACKAGE_VERSIONS.ID.eq(Modules.MODULES.PACKAGE_VERSION_ID))
                    .and(Packages.PACKAGES.ID.eq(PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID))
                    .fetch().forEach(record -> uris.put( record.component1(),
                            "fasten://mvn!" + record.component2() + "$" + record.component3() + record.component4()));
        } catch (DataAccessException e) {
            throw new UnrecoverableError(e);
        }
        return uris;
    }
}