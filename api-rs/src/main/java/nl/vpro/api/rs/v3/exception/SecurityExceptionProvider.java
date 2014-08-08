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
public class SecurityExceptionProvider implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(SecurityException exception) {
        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getMessage())).status(Response.Status.UNAUTHORIZED).build();
    }

}
