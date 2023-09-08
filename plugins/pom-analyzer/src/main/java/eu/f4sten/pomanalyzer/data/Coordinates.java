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
package eu.f4sten.pomanalyzer.data;

import dev.c0ps.maven.data.Pom;
import dev.c0ps.maveneasyindex.Artifact;

public class Coordinates {

    // with packaging (g:a:jar:v)
    public static String toCoordinate(Artifact id) {
        return create(id.groupId, id.artifactId, id.packaging, id.version);
    }

    // without packaging (g:a:?:v)
    public static String toCoordinate(MavenId id) {
        return create(id.groupId, id.artifactId, "?", id.version);
    }

    // with packaging (g:a:jar:v)
    public static String toCoordinate(Pom pom) {
        return create(pom.groupId, pom.artifactId, pom.packagingType, pom.version);
    }

    private static String create(String g, String a, String p, String v) {
        return new StringBuilder() //
                .append(g).append(':') //
                .append(a).append(':') //
                .append(p).append(':') //
                .append(v).toString();
    }
}
