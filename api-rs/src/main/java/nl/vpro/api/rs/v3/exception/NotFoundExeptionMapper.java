/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.util.rs.error.NotFoundException;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
public class NotFoundExeptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return Response
                .ok(new nl.vpro.domain.api.Error(Response.Status.NOT_FOUND.getStatusCode(), exception.getMessage()))
                .status(Response.Status.NOT_FOUND)
                .build();
    }

}
