package com.hubspot.dropwizard.guice.sample.command;

import com.hubspot.dropwizard.guice.InjectedConfiguredCommand;
import com.hubspot.dropwizard.guice.Run;
import com.hubspot.dropwizard.guice.sample.HelloWorldConfiguration;

import javax.inject.Named;

public class SimpleCommand extends InjectedConfiguredCommand<HelloWorldConfiguration> {
    public static String configName;

    public SimpleCommand() {
        super("SimpleCommand", "A command that does not do much.");
    }

    @Run
    public void run(@Named("defaultName") String name) {
        configName = name;
    }
}
