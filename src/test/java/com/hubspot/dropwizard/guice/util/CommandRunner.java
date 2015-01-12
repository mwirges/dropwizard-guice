package com.hubspot.dropwizard.guice.util;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.junit.ConfigOverride;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.http.client.params.ClientPNames;

import java.util.Enumeration;

public class CommandRunner<C extends Configuration> {
    private final Class<? extends Application<C>> applicationClass;
    private final String configPath;
    private final String commandName;
    private final Command command;
    private final ConfigOverride[] configOverrides;

    private Application<C> application;
    private Bootstrap<C> bootstrap;
    private Namespace namespace;

    public CommandRunner(Class<? extends Application<C>> applicationClass,
                             String configPath,
                             String commandName,
                             ConfigOverride... configOverrides) {
        this.applicationClass = applicationClass;
        this.configPath = configPath;
        this.commandName = commandName;
        this.command = null;
        this.configOverrides = configOverrides;
    }

    public CommandRunner(Class<? extends Application<C>> applicationClass,
                         String configPath,
                         Command command,
                         ConfigOverride... configOverrides) {
        this.applicationClass = applicationClass;
        this.configPath = configPath;
        this.commandName = null;
        this.command = command;
        this.configOverrides = configOverrides;
    }

    private void setConfigOverrides() {
        for (ConfigOverride configOverride: configOverrides) {
            configOverride.addToSystemProperties();
        }
    }

    private void resetConfigOverrides() {
        for (Enumeration<?> props = System.getProperties().propertyNames(); props.hasMoreElements();) {
            String keyString = (String) props.nextElement();
            if (keyString.startsWith("dw.")) {
                System.clearProperty(keyString);
            }
        }
    }

    public Application<C> newApplication() {
        try {
            return application = applicationClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Bootstrap<C> newBootStrap() {
        if(application == null) throw new RuntimeException("Application must be initialized before newBootStrap is called.");
        return bootstrap = new Bootstrap<C>(application);
    }

    private void initialize() {
        newApplication();
        newBootStrap();
        namespace = new Namespace(ImmutableMap.<String, Object>of("file", configPath));

        application.initialize(bootstrap);
    }

    private Command getCommand(String name) {
        if(bootstrap == null) throw new RuntimeException("Must be initialized before getCommand is called.");
        for(Command command : bootstrap.getCommands()) {
            if(command.getName() == name) return command;
        }
        return null;
    }

    public void run() {
        setConfigOverrides();
        initialize();

        try {
            if (command != null) command.run(bootstrap, namespace);
            else getCommand(commandName).run(bootstrap, namespace);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        resetConfigOverrides();
    }
}
