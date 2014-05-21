/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class IllegalArgumentProvider implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.BAD_REQUEST.getStatusCode(), exception.getMessage())).status(Response.Status.BAD_REQUEST).build();
    }

}
