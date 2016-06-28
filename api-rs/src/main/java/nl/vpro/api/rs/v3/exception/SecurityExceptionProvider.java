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

import nl.vpro.api.rs.v3.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class SecurityExceptionProvider implements ExceptionMapper<SecurityException> {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityExceptionProvider.class);


    @Override
    public Response toResponse(SecurityException exception) {
        LOG.info("Security exception: {}. Request: {}", exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
                .status(FORBIDDEN)
                .entity(new nl.vpro.domain.api.Error(FORBIDDEN, exception.getMessage()))
                .build();
    }

}
