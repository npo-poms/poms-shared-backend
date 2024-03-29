/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;


import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.xml.sax.SAXParseException;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
@Log4j2
public class BadRequestMapper implements ExceptionMapper<BadRequestException> {


    @Override
    public Response toResponse(BadRequestException exception) {
        Throwable cause = exception;
        if (exception.getCause() != null) {
            cause = exception.getCause();
        }
        String message = cause.getMessage();
        if (message == null) {
            cause = cause.getCause();
            if (cause != null) {
                if (cause instanceof SAXParseException) {
                    message = cause.toString();
                } else {
                    message = cause.getMessage();
                }
            }
        }
        log.info("Bad request: {}. Request: {}", message, StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, message))
                .build();

    }

}
