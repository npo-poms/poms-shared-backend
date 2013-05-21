/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.exception;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.Failure;

import nl.vpro.domain.api.Error;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ServerError extends Failure {
    private final static int CODE = 500;

    public ServerError(String message) {
        super(message, Response.ok(new Error(CODE, message)).status(CODE).build());
    }

    public ServerError(Throwable throwable) {
        super(throwable, Response.ok(new Error(CODE, throwable.getMessage())).status(CODE).build());
    }
}
