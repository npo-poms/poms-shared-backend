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

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
@Slf4j
public class SecurityExceptionProvider implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(SecurityException exception) {
        log.info("Security exception: {}. Request: {}", exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(FORBIDDEN)
                .entity(new nl.vpro.domain.api.Error(FORBIDDEN, exception.getMessage()))
                .build();
    }

}
