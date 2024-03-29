/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Michiel Meeuwissen
 * @since 5.31
 */
@Provider
public class ProfileNotFoundExceptionMapper implements ExceptionMapper<ProfileNotFoundException> {

    @Override
    public Response toResponse(ProfileNotFoundException exception) {
        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, exception, false, true))
                .build();
    }

}
