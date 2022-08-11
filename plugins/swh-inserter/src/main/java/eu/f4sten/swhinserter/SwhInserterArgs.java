package eu.f4sten.swhinserter;

import com.beust.jcommander.Parameter;
import eu.f4sten.infra.kafka.DefaultTopics;

public class SwhInserterArgs {
        @Parameter(names = "--swhinserter.kafkaIn", arity = 1)
        public String kafkaIn = DefaultTopics.SOURCES_PROVIDER;
}