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

package eu.f4sten.vulchainfinder.utils;

import com.google.gson.reflect.TypeToken;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.metadatadb.codegen.tables.Modules;
import eu.fasten.core.data.vulnerability.Vulnerability;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jooq.DSLContext;
import org.jooq.Record;

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

    public Map<FastenURI, List<Vulnerability>> selectVulCallablesOf(final Set<Long> depIds) {
        Map<FastenURI, List<Vulnerability>> vulCallables = new HashMap<>();
        for (Long depId : depIds) {
            vulCallables = selectVulCallablesOf(depId);
        }
        return vulCallables;
    }

    public Map<FastenURI, List<Vulnerability>> selectVulCallablesOf(final long depId) {

        final var moduleIds = selectAllModulesOf(depId);
        if (moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return selectConcurrentlyVulCallablesOf(moduleIds);
    }

    public Map<FastenURI, List<Vulnerability>> selectConcurrentlyVulCallablesOf(
        final Set<Long> moduleIds) {
        Map<FastenURI, List<Vulnerability>> result = new ConcurrentHashMap<>();
        moduleIds.parallelStream().forEach(moduleId ->
            context.fetch(selectVulCallablesWhereModuleIdIs(moduleId))
                .forEach(record -> {
                    final var vulField = record.get(3);
                    if (vulField == null) {
                        return;
                    }
                    
                    final var setType =
                        new TypeToken<HashMap<String, Vulnerability>>() {}.getType();
                    Map<String, Vulnerability> vulIdVulObject =
                        jsonUtils.fromJson(vulField.toString(), setType);

                    result.put(createFastenUri(record), new ArrayList<>(vulIdVulObject.values()));
                }));
        return result;
    }

    public FastenURI createFastenUri(final Record record) {
        final var uriString =
            String.format("%s%s$%s%s",
                "fasten://mvn!", record.get(0), record.get(1), record.get(2));
        return FastenURI.create(uriString);
    }

    public String selectVulCallablesWhereModuleIdIs(final Long moduleId) {
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
}