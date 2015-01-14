package com.hubspot.dropwizard.guice;

import com.hubspot.dropwizard.guice.sample.HelloWorldApplication;
import com.hubspot.dropwizard.guice.sample.HelloWorldConfiguration;
import com.hubspot.dropwizard.guice.sample.command.SimpleCommand;
import com.hubspot.dropwizard.guice.util.CommandRunner;
import org.junit.Test;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.junit.Assert.*;

public class InjectedConfiguredCommandTest {
    @Test
    public void run_simple_command() {
        new CommandRunner<HelloWorldConfiguration>(HelloWorldApplication.class, resourceFilePath("hello-world.yml"), "SimpleCommand").run();
        assertEquals("Joe", SimpleCommand.configName);
    }
}
