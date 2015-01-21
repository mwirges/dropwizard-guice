package com.hubspot.dropwizard.guice.sample.guice;

import javax.inject.Inject;
import javax.inject.Named;

//This is broken out in order to test Just In Time binding.
//This class should be available to Resources without an explicit binding statement.
public class ConfigData {
    @Inject
    @Named("template")
    private String template;

    @Inject
    @Named("defaultName")
    private String defaultName;

    public String getTemplate() { return template; }

    public String getDefaultName() { return defaultName; }
}
