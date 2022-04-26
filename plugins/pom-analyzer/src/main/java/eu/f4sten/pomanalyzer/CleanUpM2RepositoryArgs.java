package eu.f4sten.pomanalyzer;

import com.beust.jcommander.Parameter;

public class CleanUpM2RepositoryArgs {

    @Parameter(names= "--path.m2", arity = 1)
    public String pathM2 = "~/.m2/repository";

}
