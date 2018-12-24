/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.slf4j.helpers.MessageFormatter;

import nl.vpro.domain.api.Constants;
import nl.vpro.domain.constraint.PredicateTestResult;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class Exceptions {

    public static NotFoundException notFound(String message, Object... args) {
        return new NotFoundException(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    public static NotFoundException notFound(PredicateTestResult<?> testResult) {
        return new NotFoundInProfileException(testResult);
    }

    public static BadRequestException badRequest(String message, Object... args) {
        return new BadRequestException(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    public static Integer handleTooManyResultsWithDefault(long  offset, Integer max) {
        return handleTooManyResults(offset, max, Constants.MAX_RESULTS);
    }
    public static Integer handleTooManyResults(long offset, Integer max, Integer maxResults) {
        if (max == null){
            max = maxResults;
        }
        if (max > maxResults) {
            throw badRequest("Requesting more than {} results is not allowed. Use a pager!", maxResults);
        }
        if (offset + max > 10000) { // See ES max_result_window setting. This is the default, we may want to find out what the _actual_ configured value is
            throw badRequest("Offset + max may not be more than 10000. Use a query or the iterate call.");
        }
        return max;

    }
}
