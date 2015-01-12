package com.hubspot.dropwizard.guice;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guice.sample.HelloWorldApplication;
import com.hubspot.dropwizard.guice.sample.HelloWorldConfiguration;
import com.hubspot.dropwizard.guice.sample.OtherConfig;
import com.hubspot.dropwizard.guice.sample.command.SimpleCommand;
import com.hubspot.dropwizard.guice.util.CommandRunner;
import com.jayway.restassured.RestAssured;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class InjectedConfiguredCommandTest {
    @Test
    public void run_simple_command() {
        new CommandRunner<HelloWorldConfiguration>(HelloWorldApplication.class, "src/test/resources/hello-world.yml", "SimpleCommand").run();
        assertEquals("Joe", SimpleCommand.configName);
    }
}
