/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import org.jboss.resteasy.spi.BadRequestException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class BadRequestProvider implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException exception) {
        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.BAD_REQUEST.getStatusCode(), exception.getCause().getMessage())).status(Response.Status.BAD_REQUEST).build();

    }

}
