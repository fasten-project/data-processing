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
package eu.f4sten.depgraph;

import com.beust.jcommander.Parameter;

public class DepGraphArgs {

    @Parameter(names = "--depgraph.minNumExport", arity = 1, description = "Minimum number of new messages before updating disk export")
    public int minNumExport = 20000;

    @Parameter(names = "--depgraph.minTimeExportMS", arity = 1, description = "Minimum time that needs to pass before updating disk export (ms)")
    public long minTimeExportMS = 20 * 60 * 1000; // 20min
}