/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.slf4j.helpers.MessageFormatter;

import nl.vpro.domain.api.Constants;

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

    public static Integer handleTooManyResults(Integer max) {
        return handleTooManyResults(max, Constants.MAX_RESULTS);
    }
    public static Integer handleTooManyResults(Integer max, Integer maxResults) {
        if (max == null){
            return maxResults;
        }
        if (maxResults != null && max > maxResults) {
            throw badRequest("Requesting more than {} results is not allowed. Use a pager!", maxResults);
        }
        return max;

    }
}
