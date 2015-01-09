package com.hubspot.dropwizard.guice;

import com.hubspot.dropwizard.guice.sample.HelloWorldApplication;
import com.hubspot.dropwizard.guice.sample.HelloWorldConfiguration;
import com.jayway.restassured.RestAssured;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

public class IntegrationTest {
    @ClassRule
    public static final DropwizardAppRule<HelloWorldConfiguration> RULE =
            new DropwizardAppRule<HelloWorldConfiguration>(HelloWorldApplication.class, "src/test/resources/hello-world.yml");

    @BeforeClass
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:" + RULE.getLocalPort();
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
}
