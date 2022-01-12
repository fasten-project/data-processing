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
package eu.f4sten.pomanalyzer;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.pomanalyzer.data.PomAnalysisResult;
import eu.f4sten.pomanalyzer.data.ResolutionResult;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import eu.f4sten.pomanalyzer.utils.EffectiveModelBuilder;
import eu.f4sten.pomanalyzer.utils.MavenRepositoryUtils;
import eu.f4sten.pomanalyzer.utils.PomExtractor;
import eu.f4sten.pomanalyzer.utils.Resolver;
import eu.f4sten.server.core.AssertArgs;
import eu.f4sten.server.core.Plugin;
import eu.f4sten.server.core.utils.Kafka;
import eu.f4sten.server.core.utils.Lane;
import eu.fasten.core.maven.utils.MavenUtilities;
import eu.fasten.core.utils.Asserts;

public class POMAnalyzer implements Plugin {

	private static final Logger LOG = LoggerFactory.getLogger(POMAnalyzer.class);

	private final MavenRepositoryUtils repo;
	private final EffectiveModelBuilder modelBuilder;
	private final PomExtractor extractor;
	private final DatabaseUtils db;
	private final Resolver resolver;
	private final Kafka kafka;
	private final MyArgs args;

	@Inject
	public POMAnalyzer(MavenRepositoryUtils repo, EffectiveModelBuilder modelBuilder, PomExtractor extractor,
			DatabaseUtils db, Resolver resolver, Kafka kafka, MyArgs args) {
		this.repo = repo;
		this.modelBuilder = modelBuilder;
		this.extractor = extractor;
		this.db = db;
		this.resolver = resolver;
		this.kafka = kafka;
		this.args = args;
	}

	@Override
	public void run() {
		AssertArgs.assertFor(args)//
				.notNull(a -> a.kafkaIn, "kafka input topic") //
				.notNull(a -> a.kafkaOut, "kafka output topic");

		LOG.info("Subscribing to '{}', will publish in '{}' ...", args.kafkaIn, args.kafkaOut);

		kafka.subscribe(args.kafkaIn, MavenId.class, (id, lane) -> {
			LOG.info("Consuming next record ...");
			var artifact = bootstrapFirstResolutionResultFromInput(id);
			if (!artifact.localPomFile.exists()) {
				artifact.localPomFile = repo.downloadPomToTemp(artifact);
			}
			process(artifact, lane);
		});
	}

	private static ResolutionResult bootstrapFirstResolutionResultFromInput(MavenId id) {
		Asserts.assertNotNull(id.groupId);
		Asserts.assertNotNull(id.artifactId);
		Asserts.assertNotNull(id.version);

		var groupId = id.groupId.strip();
		var artifactId = id.artifactId.strip();
		var version = id.version.strip();
		var coord = asMavenCoordinate(groupId, artifactId, version);

		String artifactRepository = MavenUtilities.MAVEN_CENTRAL_REPO;
		if (id.artifactRepository != null) {
			var val = id.artifactRepository.strip();
			if (!val.isEmpty()) {
				artifactRepository = val;
			}
		}
		return new ResolutionResult(coord, artifactRepository);
	}

	private static String asMavenCoordinate(String groupId, String artifactId, String version) {
		// packing type is unknown
		return String.format("%s:%s:?:%s", groupId, artifactId, version);
	}

	private void process(ResolutionResult artifact, Lane lane) {
		LOG.info("Processing {} ...", artifact.coordinate);

		// resolve dependencies to
		// 1) have dependencies
		// 2) identify artifact sources
		// 3) make sure all dependencies exist in local .m2 folder
		var deps = resolver.resolveDependenciesFromPom(artifact.localPomFile);

		// merge pom with all its parents and resolve properties
		Model m = modelBuilder.buildEffectiveModel(artifact.localPomFile);

		// extract contents of pom file
		var result = extractor.process(m);

		// remember source repository for artifact
		result.artifactRepository = artifact.artifactRepository;

		result.sourcesUrl = repo.getSourceUrlIfExisting(result);
		result.releaseDate = repo.getReleaseDate(result);

		store(result, lane);

		// resolution can be different for dependencies, so process them independently
		deps.forEach(dep -> {
			process(dep, lane);
		});
	}

	private void store(PomAnalysisResult result, Lane lane) {
		db.save(result);
		if (lane == Lane.PRIORITY) {
			db.markAsIngestedPackage(result);
		}
		kafka.publish(result, args.kafkaOut, lane);
	}

}