/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import nl.vpro.api.rs.v3.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
@Provider
public class AccessDeniedExceptionProvider implements ExceptionMapper<AccessDeniedException> {

    private static final Logger LOG = LoggerFactory.getLogger(AccessDeniedExceptionProvider.class);


    @Override
    public Response toResponse(AccessDeniedException exception) {
        LOG.info("Access denied exception: {}. Request: {}", exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
            .status(UNAUTHORIZED)
            .entity(new nl.vpro.domain.api.Error(UNAUTHORIZED, exception.getMessage()))
            .build();
    }
}
