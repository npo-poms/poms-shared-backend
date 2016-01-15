/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;


import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.xml.sax.SAXParseException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Provider
public class BadRequestProvider implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException exception) {
        Throwable cause = exception;
        if (exception.getCause() != null) {
            cause = exception.getCause();
        }
        String message = cause.getMessage();
        if (message == null) {
            cause = cause.getCause();
            if (cause != null) {
                if (cause instanceof SAXParseException) {
                    message = cause.toString();
                } else {
                    message = cause.getMessage();
                }
            }
        }
        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, message))
                .build();

    }

}
