/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
@Provider
@Slf4j
public class JsonMappingExceptionProvider implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException exception) {
        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, exception.getMessage()))
                .build();
    }

}
