/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.springframework.security.core.AuthenticationException;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * @author Michiel Meeuwissen
 * @since 3.0
 */
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    @Override
    public Response toResponse(AuthenticationException exception) {
        return Response
                .status(UNAUTHORIZED)
                .entity(new nl.vpro.domain.api.Error(UNAUTHORIZED, exception.getMessage()))
                .build();
    }

}
