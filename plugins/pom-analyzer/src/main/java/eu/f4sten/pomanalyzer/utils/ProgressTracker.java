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

import static eu.f4sten.infra.kafka.Lane.NORMAL;
import static eu.f4sten.infra.kafka.Lane.PRIORITY;
import static java.lang.String.format;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;

import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.ResolutionResult;

public class ProgressTracker {

    private static final int MAX_RETRIES = 3;

    private final Set<String> ingested = new HashSet<>();
    private final DatabaseUtils db;

    private MavenId curOriginal;

    @Inject
    public ProgressTracker(DatabaseUtils db) {
        this.db = db;
    }

    public void startNextOriginal(MavenId original) {
        this.curOriginal = original;
    }

    public MavenId getCurrentOriginal() {
        return curOriginal;
    }

    public void registerRetry(ResolutionResult artifact, Lane lane) {
        db.registerRetry(toKey(artifact.coordinate, lane));
    }

    public void pruneRetries(ResolutionResult artifact, Lane lane) {
        db.pruneRetries(toKey(artifact.coordinate, lane));
    }

    private boolean isRetryCountExceeded(String coordinate, Lane lane) {
        return db.getRetryCount(toKey(coordinate, lane)) > MAX_RETRIES;
    }

    public boolean shouldSkip(ResolutionResult artifact, Lane lane) {
        var c = artifact.coordinate;
        return existsInMemory(c, lane) || existsInDatabase(c, lane) || isRetryCountExceeded(c, lane);
    }

    public void executionCrash(ResolutionResult artifact, Lane lane) {
        // if execution crashes, prevent re-try for both lanes
        pruneRetries(artifact, lane);
        markCompletionInMem(artifact.coordinate, NORMAL);
        markCompletionInMem(artifact.coordinate, PRIORITY);
    }

    public void markCompletionInMem(String coord, Lane lane) {
        ingested.add(toKey(coord, lane));
    }

    /* move mem-mark to DB, remove count */
    public void markCompletionInDb(String coord, Lane lane) {
        db.markAsIngestedPackage(coord, lane);
        ingested.remove(toKey(coord, lane));
    }

    private boolean existsInMemory(String coordinate, Lane lane) {
        return lane == NORMAL
                ? ingested.contains(toKey(coordinate, NORMAL)) || ingested.contains(toKey(coordinate, PRIORITY))
                : ingested.contains(toKey(coordinate, lane));
    }

    public boolean existsInDatabase(String coordinate, Lane lane) {
        return lane == NORMAL
                ? db.hasPackageBeenIngested(coordinate, NORMAL) || db.hasPackageBeenIngested(coordinate, PRIORITY)
                : db.hasPackageBeenIngested(coordinate, lane);
    }

    private static String toKey(String coordinate, Lane lane) {
        return format("%s-%s", coordinate, lane);
    }
}