/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.jackson;

import nl.vpro.jackson.MediaMapper;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * User: rico
 * Date: 05/04/2012
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {

    private transient Logger log = LoggerFactory.getLogger(JacksonContextResolver.class);
    private ObjectMapper objectMapper;

    public JacksonContextResolver() throws JsonGenerationException, JsonMappingException, IOException {
        log.info("Using my own Jackson ObjectMapper");
        objectMapper = new MediaMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}