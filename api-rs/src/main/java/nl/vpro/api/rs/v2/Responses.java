/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2;

import javax.ws.rs.core.Response;

import org.slf4j.helpers.MessageFormatter;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 * @deprecated use exceptions for error handling
 */
@Deprecated
public class Responses {

    private final static int CLIENT_ERROR = 400;

    private final static int SERVER_ERROR = 500;

    public static Response mediaNotFound(String id) {
        return Response.ok(new nl.vpro.domain.api.Error(CLIENT_ERROR, "No media for id " + id)).status(CLIENT_ERROR).build();
    }

    public static Response pageNotFound(String id) {
        return Response.ok(new nl.vpro.domain.api.Error(CLIENT_ERROR, "No page for id " + id)).status(CLIENT_ERROR).build();
    }

    public static Response profileNotFound(String name) {
        return Response.ok(new nl.vpro.domain.api.Error(CLIENT_ERROR, "No profile for name " + name)).status(CLIENT_ERROR).build();
    }

    public static Response clientError(String message, String... args) {
        return Response.ok(new nl.vpro.domain.api.Error(CLIENT_ERROR, MessageFormatter.arrayFormat(message, args).getMessage())).status(CLIENT_ERROR).build();
    }

    public static Response serverError(String message, String... args) {
        return Response.ok(new nl.vpro.domain.api.Error(SERVER_ERROR, MessageFormatter.arrayFormat(message, args).getMessage())).status(SERVER_ERROR).build();
    }


}
