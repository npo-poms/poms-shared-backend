/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.exception;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.Failure;

import nl.vpro.domain.api.Error;

/**
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Deprecated
public class BadRequest extends Failure {

    private final static int CODE = 400;

    public BadRequest(String message) {
        super(message, Response.ok(new Error(CODE, message)).status(CODE).build());
    }

    public BadRequest(Throwable throwable) {
        super(throwable, Response.ok(new Error(CODE, throwable.getMessage())).status(CODE).build());
    }
}
