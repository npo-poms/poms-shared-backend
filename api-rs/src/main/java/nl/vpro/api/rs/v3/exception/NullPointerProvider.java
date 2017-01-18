/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.api.rs.v3.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
@Slf4j
public class NullPointerProvider implements ExceptionMapper<NullPointerException> {
    @Override
    public Response toResponse(NullPointerException exception) {
        log.error("Wrapped a null pointer. Request: {}", StoreRequestInThreadLocal.getRequestBody(), exception);
        return Response
                .status(INTERNAL_SERVER_ERROR)
                .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                .build();
    }

}
