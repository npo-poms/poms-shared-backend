/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class IllegalArgumentProvider implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, exception))
                .build();
    }

}
