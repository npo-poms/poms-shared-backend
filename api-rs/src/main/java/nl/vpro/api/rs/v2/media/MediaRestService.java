/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Endpoint which facilitates RPC like requests on media content. This API intents to capture meaningful and frequent
 * queries on media used when building a site or apps containing POMS media. This not a real REST API. It has no update
 * statements and it is mainly document oriented. Most calls will return a full media document and there are no separate
 * calls for sub-resources.
 * <p/>
 * The API returns three media instance pageTypes: Programs, Groups and Segments. A Program result always includes it's
 * contained Segments, but it is possible to retrieve Segments on there own. This is useful when a Segment occurs
 * on a playlist for example.
 * <p/>
 * Media id's may be either a full urn or a mid. Retrieval by crid is not implemented at this moment.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path(MediaRestService.PATH)
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface MediaRestService {
    public static final String PATH = "/media";

    @GET
    MediaResult list(
        @QueryParam("properties") String properties,
        @QueryParam("sort") @DefaultValue("asc") String sort,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @POST
    MediaSearchResult find(
        MediaForm form,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/{id}")
    MediaObject load(
        @PathParam("id") String id,
        @QueryParam("properties") String properties
    );

    @GET
    @Path("/{id}/members")
    MediaResult listMembers(
        @PathParam("id") String id,
        @QueryParam("properties") String properties,
        @QueryParam("sort") @DefaultValue("asc") String sort,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @POST
    @Path("/{id}/members")
    MediaSearchResult findMembers(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    /**
     *
     * @param id      existing urn or mid
     * @param offset
     * @param max
     * @return
     */
    @GET
    @Path("/{id}/episodes")
    ProgramResult listEpisodes(
        @PathParam("id") String id,
        @QueryParam("properties") String properties,
        @QueryParam("sort") @DefaultValue("asc") String sort,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    /**
     *
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @return
     */
    @POST
    @Path("/{id}/episodes")
    ProgramSearchResult findEpisodes(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/{id}/descendants")
    MediaResult listDescendants(
        @PathParam("id") String id,
        @QueryParam("properties") String properties,
        @QueryParam("sort") @DefaultValue("asc") String sort,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @POST
    @Path("/{id}/descendants")
    MediaSearchResult findDescendants(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/{id}/related")
    MediaResult listRelated(
        @PathParam("id") String id,
        @QueryParam("properties") String properties,
        @QueryParam("sort") @DefaultValue("asc") String sort,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @POST
    @Path("/{id}/related")
    MediaSearchResult findRelated(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/changes")
    Change changes(
        @QueryParam("profile") String profile,
        @QueryParam("since") Long since,
        @QueryParam("order") @DefaultValue("asc") String order,
        @QueryParam("max") Integer max,
        @Context HttpServletRequest request,
        @Context HttpServletResponse response) throws IOException;
}
