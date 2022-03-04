/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;


import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
@Provider
@Slf4j
public class UnsupportedOperationMapper implements ExceptionMapper<UnsupportedOperationException> {


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
        log.info("Not implemented: {}. Request: {}", message, StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(NOT_IMPLEMENTED)
                .entity(new nl.vpro.domain.api.Error(NOT_IMPLEMENTED, message))
                .build();

    }

}
