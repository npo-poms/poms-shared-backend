/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import nl.vpro.domain.NotFoundException;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return Response
                .status(NOT_FOUND)
                .entity(new nl.vpro.domain.api.Error(NOT_FOUND, exception.getMessage()))
                .build();
    }

}
