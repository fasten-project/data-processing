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
package eu.f4sten.vulchainfinderdev;

import com.beust.jcommander.Parameter;
import eu.f4sten.infra.kafka.DefaultTopics;

import java.io.File;

public class VulChainFinderArgs {

    @Parameter(names = "--vulchainfinder.kafkaIn", arity = 1)
    public String kafkaIn = DefaultTopics.CALLABLE_INDEXER;

    @Parameter(names = "--vulchainfinder.kafkaOut", arity = 1)
    public String kafkaOut = DefaultTopics.VUL_CHAIN_FINDER;

    @Parameter(names = "--restApiBaseUrl", arity = 1)
    public String restApiBaseURL;

    @Parameter(names = "--depResolverBaseUrl", arity = 1)
    public String depResolverBaseURL;

    @Parameter(names = "--callableIndexPath", arity = 1)
    public File callableIndexPath;

    @Parameter(names = "--vulnChainRepoPath", arity = 1)
    public File vulnChainRepoPath;
}
