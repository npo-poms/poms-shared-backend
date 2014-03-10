/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

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
public class NulPointerProvider implements ExceptionMapper<NullPointerException> {
    private static final Logger log = LoggerFactory.getLogger(NulPointerProvider.class);

    @Override
    public Response toResponse(NullPointerException exception) {
        log.error("Wrapped a null pointer", exception);
        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage())).status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

}
