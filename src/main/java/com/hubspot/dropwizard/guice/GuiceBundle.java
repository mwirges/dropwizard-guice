package com.hubspot.dropwizard.guice;

import java.util.List;

import com.google.inject.*;
import io.dropwizard.setup.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class GuiceBundle<T extends Configuration> implements ConfiguredBundle<T> {

    final Logger logger = LoggerFactory.getLogger(GuiceBundle.class);

    private final AutoConfig autoConfig;
    private final List<Module> modules;
    private final List<Module> initModules;
    private final List<Function<Injector, ServletContextListener>> contextListenerGenerators;
    private final String[] configurationPackages;
    private final InjectorFactory injectorFactory;

    private Injector initInjector;
    private Injector injector;
    private DropwizardEnvironmentModule dropwizardEnvironmentModule;
    private Optional<Class<T>> configurationClass;
    private Stage stage;

    public static class Builder<T extends Configuration> {
        private AutoConfig autoConfig;
        private List<Module> initModules = Lists.newArrayList();
        private List<Module> modules = Lists.newArrayList();
        private List<Function<Injector, ServletContextListener>> contextListenerGenerators = Lists.newArrayList();
        private Optional<Class<T>> configurationClass = Optional.absent();
        private InjectorFactory injectorFactory = new InjectorFactoryImpl();
        List<String> configurationPackages = new ArrayList<String>();

        /**
         * Add a module to the bundle.
         * Module may be injected with configuration and environment data.
         * This module will NOT be available for other Bundles and Commands initialized with AutoConfig.
         * Modules will also NOT be available when running classic Command and ConfiguredCommands.
         * They will be available when using InjectedConfiguredCommand, however.
         */
        public Builder<T> addModule(Module module) {
            Preconditions.checkNotNull(module);
            modules.add(module);
            return this;
        }

        /**
         * Add a module to the bundle.
         * Module will not be injected itself.
         * This module will be available for other Bundles and Commands
         * initialized with AutoConfig.
         */
        public Builder<T> addInitModule(Module module) {
            Preconditions.checkNotNull(module);
            initModules.add(module);
            return this;
        }

        public Builder<T> addServletContextListener(Function<Injector, ServletContextListener> contextListenerGenerator) {
            Preconditions.checkNotNull(contextListenerGenerator);
            contextListenerGenerators.add(contextListenerGenerator);
            return this;
        }

        public Builder<T> setConfigClass(Class<T> clazz) {
            configurationClass = Optional.of(clazz);
            return this;
        }

        /**
         * Sets a list of base packages that may contain configuration objects.
         * When config data is bound in the injector, classes within these
         * packages will be recursed into.
         */
        public Builder<T> addConfigPackages(String... basePackages) {
            Preconditions.checkNotNull(basePackages.length > 0);
            configurationPackages.addAll(Arrays.asList(basePackages));
            return this;
        }
        
        public Builder<T> setInjectorFactory(InjectorFactory factory) {
            Preconditions.checkNotNull(factory);
            injectorFactory = factory;
            return this;
        }

        public Builder<T> enableAutoConfig(String... basePackages) {
            Preconditions.checkNotNull(basePackages.length > 0);
            Preconditions.checkArgument(autoConfig == null, "autoConfig already enabled!");
            autoConfig = new AutoConfig(basePackages);
            return this;
        }

        public GuiceBundle<T> build() {
            return build(Stage.PRODUCTION);
        }

        public GuiceBundle<T> build(Stage s) {
            return new GuiceBundle<T>(s, autoConfig, modules, initModules, contextListenerGenerators, injectorFactory,
                                      configurationClass, configurationPackages.toArray(new String[0]));
        }

    }
    
    public static <T extends Configuration> Builder<T> newBuilder() {
        return new Builder<T>();
    }

    private GuiceBundle(Stage stage,
                        AutoConfig autoConfig,
                        List<Module> modules,
                        List<Module> initModules,
                        List<Function<Injector, ServletContextListener>> contextListenerGenerators,
						InjectorFactory injectorFactory,
                        Optional<Class<T>> configurationClass,
                        String[] configurationPackages) {
        Preconditions.checkNotNull(modules);
        Preconditions.checkArgument(!modules.isEmpty());
        Preconditions.checkNotNull(contextListenerGenerators);
        Preconditions.checkNotNull(stage);
        Preconditions.checkNotNull(configurationPackages);
        this.modules = modules;
        this.initModules = initModules;
        this.contextListenerGenerators = contextListenerGenerators;
        this.autoConfig = autoConfig;
        this.configurationClass = configurationClass;
        this.injectorFactory = injectorFactory;
        this.configurationPackages = configurationPackages;
        this.stage = stage;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        initInjector();
        if (autoConfig != null) {
            autoConfig.initialize(bootstrap, initInjector);
        }

        setupCommands(bootstrap.getCommands());
    }

    @SuppressWarnings("unchecked")
    private void setupCommands(Collection<Command> commands) {
        for(Command c : commands) {
            if(c instanceof GuiceCommand) {
                ((GuiceCommand) c).setInit(this);
            }
        }
    }

    private void initInjector() {
        try {
            initInjector = injectorFactory.create(this.stage, ImmutableList.copyOf(this.initModules));
        } catch(Exception ie) {
		    logger.error("Exception occurred when creating Guice Injector - exiting", ie);
		    System.exit(1);
	    }
    }

    @Override
    public void run(final T configuration, final Environment environment) {
        run(null, environment, configuration);
    }
    void run(Bootstrap<T> bootstrap, Environment environment, final T configuration) {
        initEnvironmentModule();
        setEnvironment(bootstrap, environment, configuration);
        //The secondary injected modules generally use config data.  If we are starting up a command
        //that doesn't have a configuration, loading these modules is useless at best.
        boolean addModules = configuration != null;
        initGuice(environment, addModules);

        if(environment != null) {
            JerseyUtil.registerGuiceBound(injector, environment.jersey());
            JerseyUtil.registerGuiceFilter(environment);

            for (Function<Injector, ServletContextListener> generator : contextListenerGenerators) {
                environment.servlets().addServletListeners(generator.apply(injector));
            }

            if (autoConfig != null) {
                autoConfig.run(environment, injector);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setEnvironment(Bootstrap<T> bootstrap, final Environment environment, final T configuration) {
        dropwizardEnvironmentModule.setEnvironmentData(bootstrap, environment, configuration);
    }


    void setNamespace(Namespace namespace) {
        dropwizardEnvironmentModule.setNamespace(namespace);
    }

    private void initEnvironmentModule() {
        if (configurationClass.isPresent()) {
            dropwizardEnvironmentModule = new DropwizardEnvironmentModule<T>(configurationClass.get(), configurationPackages);
        } else {
            dropwizardEnvironmentModule = new DropwizardEnvironmentModule<Configuration>(Configuration.class, configurationPackages);
        }
    }

    private void initGuice(final Environment environment, boolean addModules) {
        Injector environmentInjector = initInjector.createChildInjector(dropwizardEnvironmentModule);

        if(addModules) {
            for (Module module : modules)
                environmentInjector.injectMembers(module);

            if (environment != null) modules.add(new JerseyModule());
            injector = environmentInjector.createChildInjector(ImmutableList.copyOf(modules));
        }
        else injector = environmentInjector;
    }

    public Provider<Injector> getInjector() {
        //With double injection, it is not safe to simply provide the injector as the correct
        //instance will change over time.
        return new Provider<Injector>() {
            @Override
            public Injector get() {
                return (injector != null) ? injector : initInjector;
            }
        };
    }
}
