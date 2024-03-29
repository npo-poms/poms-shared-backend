/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
@Log4j2
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(SecurityException exception) {
        log.info("Security exception: {}. Request: {}", exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(FORBIDDEN)
                .entity(new nl.vpro.domain.api.Error(FORBIDDEN, exception.getMessage()))
                .build();
    }

}
