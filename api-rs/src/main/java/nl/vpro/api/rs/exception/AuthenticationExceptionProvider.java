/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.springframework.security.core.AuthenticationException;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * @author Michiel Meeuwissen
 * @since 3.0
 */
@Provider
public class AuthenticationExceptionProvider implements ExceptionMapper<AuthenticationException> {

    @Override
    public Response toResponse(AuthenticationException exception) {
        return Response
                .status(UNAUTHORIZED)
                .entity(new nl.vpro.domain.api.Error(UNAUTHORIZED, exception.getMessage()))
                .build();
    }

}
