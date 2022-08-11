package eu.f4sten.swhinserter;

import com.google.inject.Binder;
import eu.f4sten.infra.IInjectorConfig;
import eu.f4sten.infra.InjectorConfig;

@InjectorConfig
public class SwhInserterInjectorConfig implements IInjectorConfig {

    private SwhInserterArgs args;

    public SwhInserterInjectorConfig(SwhInserterArgs args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(SwhInserterArgs.class).toInstance(args);
    }

}
