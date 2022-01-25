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
package eu.f4sten.infra.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.json.JsonMapper.Builder;

public class ObjectMapperBuilder {

    public ObjectMapper build() {
        var builder = JsonMapper.builder() //
                .disable(MapperFeature.AUTO_DETECT_GETTERS) //
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        var om = addBuilderOptions(builder).build() //
                .setVisibility(PropertyAccessor.ALL, Visibility.ANY) //
                .setSerializationInclusion(Include.NON_NULL);

        return addMapperOptions(om);
    }

    /**
     * allow additional builder options in subclasses , e.g., for testing
     */
    protected Builder addBuilderOptions(Builder b) {
        return b;
    }

    /**
     * allow additional mapper options in subclasses , e.g., for testing
     */
    protected ObjectMapper addMapperOptions(ObjectMapper om) {
        return om;
    }
}