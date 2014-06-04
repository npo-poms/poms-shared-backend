/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.slf4j.helpers.MessageFormatter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class Exceptions {

    public static NotFoundException notFound(String message, Object... args) {
        return new NotFoundException(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    public static BadRequestException badRequest(String message, Object... args) {
        return new BadRequestException(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    public static ServerErrorException serverError(String message, Object... args) {
        return new ServerErrorException(MessageFormatter.arrayFormat(message, args).getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }

}
