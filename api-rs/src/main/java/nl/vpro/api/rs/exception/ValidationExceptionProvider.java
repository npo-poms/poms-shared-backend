/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;


import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.api.validation.ResteasyViolationException;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


/**
 * @author rico
 * @since 3.1
 */
@Provider
@Slf4j
public class ValidationExceptionProvider implements ExceptionMapper<ResteasyViolationException> {
    @Override
    public Response toResponse(final ResteasyViolationException e) {
        final String violationsAsString = e.getViolations().toString().replace('\r', ' ');

        log.info("Bad request/Violation exception: {}. Request: {}", violationsAsString, StoreRequestInThreadLocal.getRequestBody());
        nl.vpro.domain.api.Error error = new nl.vpro.domain.api.Error(BAD_REQUEST, violationsAsString);
        error.setViolations(e.getViolations());
        return Response
                .status(BAD_REQUEST)
                .entity(error)
                .build();
    }

}
