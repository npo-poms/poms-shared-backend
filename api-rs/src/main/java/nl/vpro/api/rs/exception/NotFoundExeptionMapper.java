/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.util.rs.error.NotFoundException;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
public class NotFoundExeptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return Response
                .status(NOT_FOUND)
                .entity(new nl.vpro.domain.api.Error(NOT_FOUND, exception.getMessage()))
                .build();
    }

}
