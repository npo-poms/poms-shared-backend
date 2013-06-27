/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;

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
@Path(MediaRestService.PATH)
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface MediaRestService {
    public static final String PATH = "/media";

    @GET
    MediaResult list(
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    @GET
    @Path("/changes")
    void changes(
        @QueryParam("profile") String profile,
        @QueryParam("since") Long since,
        @QueryParam("order") @DefaultValue("asc") String order,
        @QueryParam("max") Integer max,
        @Context HttpServletRequest request,
        @Context HttpServletResponse response) throws IOException;

    @POST
    MediaSearchResult find(
        MediaForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * Retrieve a media resource, either a Program, Group or Segment, by it's id.
     *
     * @param id   existing urn or mid
     * @param mock whether to return a mock object
     * @return full Program, Group or Segment
     */
    @GET
    @Path("/{id}")
    MediaObject load(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @GET
    @Path("/{id}/members")
    MediaResult listMembers(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @POST
    @Path("/{id}/members")
    MediaSearchResult findMembers(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @GET
    @Path("/{id}/episodes")
    ProgramResult listEpisodes(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @POST
    @Path("/{id}/episodes")
    ProgramSearchResult findEpisodes(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @GET
    @Path("/{id}/descendants")
    MediaResult listDescendants(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @POST
    @Path("/{id}/descendants")
    MediaSearchResult findDescendants(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @GET
    @Path("/{id}/related")
    MediaResult listRelated(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * @param id      existing urn or mid
     * @param profile
     * @param offset
     * @param max
     * @param mock
     * @return
     */
    @POST
    @Path("/{id}/related")
    MediaSearchResult findRelated(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);
}
