package com.hubspot.dropwizard.guice.sample;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;

import javax.inject.Named;

public class DependentModule extends AbstractModule {

    @Inject
    @Named("subConfig.moreData")
    private String injectedData;

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("dependent")).toInstance("More data is: " + injectedData);
    }
}
