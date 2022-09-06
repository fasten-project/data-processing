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
package eu.f4sten.pomanalyzer.data;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.nio.file.Paths;

public class MavenId {

    public String groupId;
    public String artifactId;
    public String version;
    public String artifactRepository;
    public String packagingType;

    public MavenId() {}

    public MavenId(String groupId, String artifactId, String version, String artifactRepository, String packagingType) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.artifactRepository = artifactRepository;
        this.packagingType = packagingType;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

    public String asCoordinate() {
        return String.format("%s:%s:%s", $(groupId), $(artifactId), $(version));
    }

    public String toJarPath() {
        return Paths.get(groupId.replace('.', '/'), artifactId, version,
                artifactId + "-" + version + "." + "jar").toString();
    }

    private static String $(String s) {
        return s == null ? "?" : s;
    }
}