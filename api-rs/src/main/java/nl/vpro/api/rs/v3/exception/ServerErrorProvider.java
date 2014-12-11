/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class ServerErrorProvider implements ExceptionMapper<ServerErrorException> {

    @Override
    public Response toResponse(ServerErrorException exception) {
        return Response.serverError().entity(
                new nl.vpro.domain.api.Error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception)).build();
    }

}
