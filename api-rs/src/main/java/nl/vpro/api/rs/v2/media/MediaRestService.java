/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.search.MediaForm;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint which facilitates RPC like requests on media content. This API intents to capture meaningful and frequent
 * queries on media used when building a site or apps containing POMS media. This not a real REST API. It has no update
 * statements and it is mainly document oriented. Most calls will return a full media document and there are no separate
 * calls for sub-resources.
 * <p/>
 * The API returns three media instance types: Programs, Groups and Segments. A Program result always includes it's
 * contained Segments, but it is possible to retrieve Segments on there own. This is useful when a Segment occurs
 * on a playlist for example.
 * <p/>
 * Media id's may be either a full urn or a mid. Retrieval by crid is not implemented at this moment.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/media")
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface MediaRestService<T extends MediaObject> {

    @GET
    @Path("/")
    PagedResult<MediaObject> list(@QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    @Path("/")
    PagedResult<MediaObject> search(MediaForm form, @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * Retrieve a media resource, either a Program, Group or Segment, by it's id.
     *
     * @param id existing urn or mid
     * @param mock whether to return a mock object
     * @return full Program, Group or Segment
     */
    @GET
    @Path("/{id}")
    MediaObject get(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock);


}
