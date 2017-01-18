/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
@Provider
public class UnsupportedOperationProvider implements ExceptionMapper<UnsupportedOperationException> {

    private static final Logger LOG = LoggerFactory.getLogger(UnsupportedOperationProvider.class);


    @Override
    public Response toResponse(UnsupportedOperationException exception) {
        Throwable cause = exception;
        if (exception.getCause() != null) {
            cause = exception.getCause();
        }
        String message = cause.getMessage();
        if (message == null) {
            cause = cause.getCause();
            if (cause != null) {
                message = cause.getMessage();
            }
        }
        LOG.info("Not implemented: {}. Request: {}", message, StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(NOT_IMPLEMENTED)
                .entity(new nl.vpro.domain.api.Error(NOT_IMPLEMENTED, message))
                .build();

    }

}
