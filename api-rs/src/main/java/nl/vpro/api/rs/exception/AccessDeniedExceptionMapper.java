/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.springframework.security.access.AccessDeniedException;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
@Provider
@Log4j2
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Response toResponse(AccessDeniedException exception) {
        log.info("Access denied exception: {}. Request: {}", exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
            .status(UNAUTHORIZED)
            .entity(new nl.vpro.domain.api.Error(UNAUTHORIZED, exception.getMessage()))
            .build();
    }
}
