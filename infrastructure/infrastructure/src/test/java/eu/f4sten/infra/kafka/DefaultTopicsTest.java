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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DefaultTopicsTest {

    @Test
    public void defaultValues() {
        assertEquals("fasten.mvn.releases", DefaultTopics.INGEST);
        assertEquals("fasten.POMAnalyzer", DefaultTopics.POM_ANALYZER);
        assertEquals("fasten.CallableIndexFastenPlugin", DefaultTopics.CALLABLE_INDEXER);
        assertEquals("fasten.VulChainFinder", DefaultTopics.VUL_CHAIN_FINDER);
    }
}