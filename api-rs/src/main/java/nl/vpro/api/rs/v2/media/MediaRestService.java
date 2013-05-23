/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
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
public interface MediaRestService {

    @GET
    @Path("/")
    Result<MediaObject> list(
            @QueryParam("profile") String profile,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
            @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    @Path("/")
    SearchResult<MediaObject> find(
        MediaForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    Result<MediaObject> listMembers(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    SearchResult<MediaObject> findMembers(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    Result<Program> listEpisodes(
            @PathParam("id") String id,
            @QueryParam("profile") String profile,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    SearchResult<Program> findEpisodes(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    Result<MediaObject> listDescendants(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    SearchResult<MediaObject> findDescendants(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    Result<MediaObject> listRelated(
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
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
    SearchResult<MediaObject> findRelated(
        MediaForm form,
        @PathParam("id") String id,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);
}
