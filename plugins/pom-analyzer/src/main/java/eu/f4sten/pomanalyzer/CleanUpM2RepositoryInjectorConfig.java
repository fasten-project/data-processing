package eu.f4sten.pomanalyzer;

import com.google.inject.Binder;
import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;

@InjectorConfig
public class CleanUpM2RepositoryInjectorConfig implements IInjectorConfig {

    private CleanUpM2RepositoryArgs args;

    public CleanUpM2RepositoryInjectorConfig(CleanUpM2RepositoryArgs args) {this.args = args;}

    @Override
    public void configure(Binder binder) {
        binder.bind(CleanUpM2RepositoryArgs.class).toInstance(args);
    }
}
