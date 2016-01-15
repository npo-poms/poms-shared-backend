/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class NullPointerProvider implements ExceptionMapper<NullPointerException> {
    private static final Logger LOG = LoggerFactory.getLogger(NullPointerProvider.class);

    @Override
    public Response toResponse(NullPointerException exception) {
        LOG.error("Wrapped a null pointer", exception);
        return Response
                .ok(new nl.vpro.domain.api.Error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception))
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }

}
