/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class GlobalExceptionProvider implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage())).status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

}
