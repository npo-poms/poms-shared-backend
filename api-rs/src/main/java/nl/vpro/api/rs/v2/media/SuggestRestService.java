/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.Suggestion;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/suggest")
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
