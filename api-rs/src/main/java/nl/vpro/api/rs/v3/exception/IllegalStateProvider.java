/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class IllegalStateProvider implements ExceptionMapper<IllegalStateException> {
    private static final Logger log = LoggerFactory.getLogger(IllegalStateProvider.class);

    @Override
    public Response toResponse(IllegalStateException exception) {
        log.error("Wrapped a illegal state", exception);
        return Response
                .status(INTERNAL_SERVER_ERROR)
                .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                .build();
    }

}
