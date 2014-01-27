/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.InternalServerErrorException;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class Exceptions {

    public static NotFoundException notFound(String message, Object... args) {
        return new NotFoundException(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    public static BadRequestException queryParamNotFound(String message, Object... args) {
        return new BadRequestException(MessageFormatter.arrayFormat(message, args).getMessage());
    }
}
