/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import nl.vpro.domain.media.MediaObject;

/**
 * Endpoint which facilitates RPC like requests on media content. This API intents to capture meaningful and frequent
 * queries on media used when building a site or apps containing POMS media. This not a real REST API. It has nu update
 * statements and it is mainly document oriented. Most calls will return a full media document and there are no separate
 * calls for sub-resources.
 *
 * The API returns three media instance types: Programs, Groups and Segments. A Program result shall always include it's
 * contained Segments, but it is possible the retrieve segments on there own. This is useful when a Segment occurs
 * on a playlist for example.
 *
 * Media id's may be either a full urn of a mid. Retrieval by crid is not implemented at this moment.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/media")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface MediaRestService<T extends MediaObject> {

    /**
     * Retrieve a media resource, either a Program, Group or Segment, by it's id.
     *
     * @param id An existing urn or mid
     * @return A full Program, Group or Segment document
     */
    @GET
    @Path("/{id}")
    @StatusCodes({
        @ResponseCode(code = 200, condition = "success"),
        @ResponseCode(code = 400, condition = "not found")})
    T get(@PathParam("id") String id);
}
