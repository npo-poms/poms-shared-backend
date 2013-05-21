/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.MediaForm;

/**
 * See https://jira.vpro.nl/browse/API-
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/mediaCompletion")
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface MediaAutoCompletion {

    @POST
    @Path("/")
    Result<String> search(
        MediaForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
        @QueryParam("max") @DefaultValue("10") Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);
}
