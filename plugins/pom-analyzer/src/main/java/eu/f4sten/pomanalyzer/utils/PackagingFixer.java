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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fasten.core.maven.data.PomAnalysisResult;

public class PackagingFixer {

    private static final Logger LOG = LoggerFactory.getLogger(PackagingFixer.class);

    private static final String[] PACKAGING_TYPES = new String[] { "jar", "war", "ear", "aar", "ejb" };

    private MavenRepositoryUtils repoUtils;

    @Inject
    public PackagingFixer(MavenRepositoryUtils repoUtils) {
        this.repoUtils = repoUtils;
    }

    public String checkPackage(PomAnalysisResult r) {

        if (repoUtils.doesExist(r)) {
            return r.packagingType;
        }

        if (!exists(r, "pom", false)) {
            LOG.warn("Neither the coordinate nor its pom can be found.");
            return r.packagingType;
        }

        var lc = r.packagingType.toLowerCase();
        var isDifferent = !r.packagingType.equals(lc);
        if (isDifferent && exists(r, lc, true)) {
            return lc;
        }

        for (var pt : PACKAGING_TYPES) {
            if (pt.equals(r.packagingType)) {
                continue;
            }
            if (exists(r, pt, true)) {
                return pt;
            }
        }

        LOG.warn("Pom exists, coordinate not found. No fix available.");
        return r.packagingType;
    }

    private boolean exists(PomAnalysisResult r, String packagingType, boolean shouldLog) {
        var clone = r.clone();
        clone.packagingType = packagingType;
        boolean doesExist = repoUtils.doesExist(clone);
        if (shouldLog && doesExist) {
            LOG.warn("Coordinate found after fixing packagingType: {} -> {}", r.packagingType, packagingType);
        }
        return doesExist;
    }
}