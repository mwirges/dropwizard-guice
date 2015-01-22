package com.hubspot.dropwizard.guice.objects;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guice.objects.ExplicitBindingService;

public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ExplicitBindingService.class);
        bindConstant().annotatedWith(Names.named("TestTaskName")).to("test task");
    }
}

