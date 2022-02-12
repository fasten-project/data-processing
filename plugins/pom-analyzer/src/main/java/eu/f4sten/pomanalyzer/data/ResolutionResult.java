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

import java.io.File;
import java.security.InvalidParameterException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;

public class ResolutionResult {

    public final File localM2Repository;

    public String coordinate; // gid:aid:packageType:version
    public String artifactRepository;
    public File localPomFile;

    public ResolutionResult(String coordinate, String artifactRepository) {
        validate(coordinate);
        this.localM2Repository = getLocalM2Repository();
        this.coordinate = coordinate;
        this.artifactRepository = artifactRepository;
        this.localPomFile = deriveLocalPomPath(localM2Repository, coordinate);
    }

    private static void validate(String coordinate) {
        var parts = coordinate.split(":");
        if (parts.length != 4) {
            throw new InvalidParameterException();
        }
        for (var i : new int[] { 0, 1, 3 }) {
            if ("".equals(parts[i]) || "?".equals(parts[i])) {
                throw new InvalidParameterException();
            }
        }
        if ("".equals(parts[2])) {
            throw new InvalidParameterException();
        }
    }

    protected File getLocalM2Repository() {
        return MavenRepositoryUtils.getPathOfLocalRepository();
    }

    public File getLocalPackageFile() {
        var packaging = coordinate.split(":")[2];
        return changeExtension(localPomFile, '.' + packaging);
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

    private static File deriveLocalPomPath(File pathM2, String coordinate) {
        var parts = coordinate.split(":");
        var g = parts[0];
        var a = parts[1];
        var v = parts[3];
        var path = pathM2.getAbsolutePath() + File.separatorChar + g.replace('.', File.separatorChar)
                + File.separatorChar + a + File.separatorChar + v + File.separatorChar + a + "-" + v + ".pom";
        return new File(path);
    }

    private static File changeExtension(File f, String extInclDot) {
        String path = f.getAbsolutePath();
        int idxOfExt = path.lastIndexOf('.');
        String newPath = path.substring(0, idxOfExt) + extInclDot;
        return new File(newPath);
    }
}