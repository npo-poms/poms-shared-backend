/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.Suggestion;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/suggest")
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON + "; charset=UTF-8", MediaType.APPLICATION_XML + "; charset=UTF-8"})
public interface SuggestRestService {

    @GET
    @Path("/")
    Result<Suggestion> all(
        @QueryParam("text") String text,
        @QueryParam("profile") String profile,
        @QueryParam("mock") @DefaultValue("false") boolean mock
    );
}
