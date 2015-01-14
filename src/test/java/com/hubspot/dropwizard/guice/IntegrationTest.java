package com.hubspot.dropwizard.guice;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guice.sample.HelloWorldApplication;
import com.hubspot.dropwizard.guice.sample.HelloWorldConfiguration;
import com.hubspot.dropwizard.guice.sample.OtherConfig;
import com.jayway.restassured.RestAssured;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static io.dropwizard.testing.ResourceHelpers.*;
import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class IntegrationTest {
    @ClassRule
    public static final DropwizardAppRule<HelloWorldConfiguration> RULE =
            new DropwizardAppRule<HelloWorldConfiguration>(HelloWorldApplication.class, resourceFilePath("hello-world.yml"));

    private static Injector injector;

    @BeforeClass
    public static void setup() {
        RestAssured.baseURI = "http://localhost:" + RULE.getLocalPort();
        injector = ((HelloWorldApplication) RULE.getApplication()).guiceBundle.getInjector().get();
    }

    @Test
    public void configuration_injection_in_resource() throws Exception {
        get("/v1/hello-world").then().body("content", equalTo("Hello, Joe!"));
    }

    @Test
    public void configuration_injection_in_healthcheck() throws Exception {
        get("admin/healthcheck").then().body("template.healthy", equalTo(true));
    }

    @Test
    public void request_injection_in_resource() throws Exception {
        get("/v1/hello-world/ctx").then().statusCode(200);
    }

    @Test
    public void module_injection_in_resource() throws Exception {
        get("/v1/hello-world/sample").then().body(equalTo("foo"));
    }

    @Test
    public void nested_configuration_injection() throws Exception {
        assertEquals("something",
                     injector.getInstance(Key.get(String.class, Names.named("subConfig.moreData"))));
    }

    @Test
    public void nested_configuration_outside_of_scope() {
        OtherConfig oc = injector.getInstance(Key.get(OtherConfig.class, Names.named("otherConfig")));
        assertEquals("hidden",oc.getOtherData());
        //Otherconfig is not within the package scope defined by 'addConfigPackages'
        //So its contents should not be avaliable.
        try {
            injector.getInstance(Key.get(String.class, Names.named("otherConfig.otherData")));
            assertFalse(true);
        } catch(ConfigurationException e) {
        }
    }

    @Test
    public void dependent_module_gets_injected() throws Exception {
        assertEquals("More data is: something",
                     injector.getInstance(Key.get(String.class, Names.named("dependent"))));
    }
}
