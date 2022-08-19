/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import lombok.extern.log4j.Log4j2;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
@Log4j2
public class ServerErrorMapper implements ExceptionMapper<ServerErrorException> {

    @Override
    public Response toResponse(ServerErrorException exception) {
        log.warn("Server error exception for request: {}", StoreRequestInThreadLocal.getRequestBody(), exception);
        return Response
                .serverError()
                .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                .build();
    }

}
