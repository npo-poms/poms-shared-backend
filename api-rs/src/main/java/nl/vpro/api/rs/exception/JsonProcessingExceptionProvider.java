/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.api.rs.interceptors.RSReaderInterceptor;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
@Provider
@Slf4j
public class JsonProcessingExceptionProvider implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        if (RSReaderInterceptor.READEXCEPTION.get()) {
            return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, exception.getMessage()))
                .build();
        } else {
            // error didn't happend during parsing of client's content. Perhaps some bug on the server (parsing ES or so?)
            return Response
                .status(INTERNAL_SERVER_ERROR)
                .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                .build();
        }
    }

}
