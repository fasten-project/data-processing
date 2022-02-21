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

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.utils.Version;
import eu.fasten.core.data.metadatadb.MetadataDao;
import org.jooq.DSLContext;

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
}