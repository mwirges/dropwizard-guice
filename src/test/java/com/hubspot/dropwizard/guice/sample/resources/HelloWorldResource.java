package com.hubspot.dropwizard.guice.sample.resources;

import com.codahale.metrics.annotation.Timed;
import com.hubspot.dropwizard.guice.sample.core.Saying;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.atomic.AtomicLong;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

	final Logger logger = LoggerFactory.getLogger(HelloWorldResource.class);

    private final String template;
    private final String defaultName;
    private final AtomicLong counter;
    private final String sample;
    private final HttpHeaders headers;

    @Inject
    private Request ctx;

    @Inject
    public HelloWorldResource(@Named("template") String template,
                              @Named("defaultName") String defaultName,
                              @Named("sample") String sample,
                              HttpHeaders headers) {
    	logger.info("Creating a new HelloWorldResource!");
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
        this.sample = sample;
        this.headers = headers;
    }

    @GET
    @Timed
    public Saying sayHello(@QueryParam("name") Optional<String> name, @Context ServletContext context) {
    	logger.info("User-Agent: " + headers.getRequestHeader("User-Agent"));
    	logger.info(Integer.toString(ctx.hashCode()));
        return new Saying(counter.incrementAndGet(),
                          String.format(template, name.or(defaultName)));
    }

    @GET
    @Timed
    @Path("ctx")
    public Response checkCTX() {
        if(ctx != null) return Response.ok().build();
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Timed
    @Path("sample")
    public String getSample() {
        return sample;
    }

    @PreDestroy
    void destroy() {
    	logger.info("Destroying HelloWorldResource... :(");
    }
}