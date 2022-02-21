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
package eu.f4sten.infra.kafka;

public class DefaultTopics {

    private DefaultTopics() {
        // do not instantiate
    }

    public static final String INGEST = "fasten.mvn.releases";
    public static final String POM_ANALYZER = "fasten.POMAnalyzer";
    public static final String CALLABLE_INDEXER = "fasten.CallableIndexFastenPlugin";
    public static final String VUL_CHAIN_FINDER = "fasten.VulChainFinder";
}