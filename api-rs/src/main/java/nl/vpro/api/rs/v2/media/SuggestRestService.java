/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.Suggestion;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/suggest")
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface SuggestRestService {

    @GET
    @Path("/")
    Result<Suggestion> all(
        @QueryParam("text") String text,
        @QueryParam("profile") String profile
    );
}
