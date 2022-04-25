package eu.f4sten.pomanalyzer;

import com.beust.jcommander.Parameter;

public class CleanUpM2RepositoryArgs {

    @Parameter(names= "--m2repo.path", arity = 1)
    public String m2RepositoryPath;

}
