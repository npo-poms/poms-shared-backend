/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import nl.vpro.domain.api.Error;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@Provider
public class NotFoundInProfileExeptionMapper implements ExceptionMapper<NotFoundInProfileException> {

    @Override
    public Response toResponse(NotFoundInProfileException exception) {
        Error error = new Error(NOT_FOUND, exception.getMessage());
        error.setTestResult(exception.getTestResult());
        return Response
                .status(NOT_FOUND)
                .entity(error)
                .build();
    }

}
