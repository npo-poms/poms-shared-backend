package nl.vpro.api.rs;

import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItemIterator;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;

/**
 * Public REST API to api.service.MediaService
 *
 * @author : maarten hogendoorn
 */

@Path(MediaRestService.PATH)
public interface MediaRestService {
    public static final String PATH = "media";

    @GET
    @Path("program/{urn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Program getProgram(@PathParam("urn") String urn);

    /**
     * Find related media for a program.
     *
     * @param programUrn The urn of the program to find related media for.
     * @param profile    The search profile used to restrict the results.
     * @param offset     Paging offset.
     * @param maxResult  Maximum number of results to return.
     * @return
     */
    @GET
    @Path("program/{urn}/related")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MediaObject> relatedForProgram(@PathParam("urn") String programUrn, @QueryParam("profile") String profile, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    @GET
    @Path("program/replay")
    @Produces(MediaType.APPLICATION_JSON)
    public ProgramList getRecentReplayablePrograms(@QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset, @QueryParam("type") String avType);

    @GET
    @Path("program/{urn}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    public Annotations getAnnotationsForProgram(@PathParam("urn") String urn);

    /**
     * if addMembers=true, members are added.
     * if addEpisodes=true, episodes are added.
     * You can filter the types of members that may be returned, by added a mediaTypes request parameter, which contains comma-seperated list of allowed mediatypes.
     * The values of the media types must be the toString values of class MediaObjectType,
     * No filtering (thus getting all members, regardless its type) is obtained by either:
     * - memberTypesFilter = null or ""
     * - memberTypesFilter = "program,group,segment" (a comma seperated list of all possible mediaObjectTypes)
     */
    @GET
    @Path("group/{urn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Group getGroup(@PathParam("urn") String urn, @QueryParam("members") @DefaultValue("false") boolean addMembers, @QueryParam("episodes") @DefaultValue("false") boolean addEpisodes, @QueryParam("membertypes") String memberTypesFilter) throws ServerErrorException, NotFoundException;

    /**
     * Find related media for a segment.
     *
     * @param groupUrn  The urn of the segment to find related media for.
     * @param profile   The search profile used to restrict the results.
     * @param offset    Paging offset.
     * @param maxResult Maximum number of results to return.
     * @return
     */
    @GET
    @Path("segment/{urn}/related")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MediaObject> relatedForSegment(@PathParam("urn") String groupUrn, @QueryParam("profile") String profile, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    @GET
    @Path("segment/{urn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Segment getSegment(@PathParam("urn") String urn) throws ServerErrorException, NotFoundException;

    /**
     * Find related media for a group.
     *
     * @param groupUrn  The urn of the program to find related media for.
     * @param profile   The search profile used to restrict the results.
     * @param offset    Paging offset.
     * @param maxResult Maximum number of results to return.
     * @return
     */
    @GET
    @Path("group/{urn}/related")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MediaObject> relatedForGroup(@PathParam("urn") String groupUrn, @QueryParam("profile") String profile, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    /**
     * Retrieve multiple media objects at once.
     *
     * @param urns id's of media objects to retrieve.
     * @return list of media objects
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public List<MediaObject> getMediaObjects(@FormParam("urn") List<String> urns);

    /**
     * Search for a certain term.
     * You can use tags to limit your search.
     *
     * @param queryString term to search on.
     * @param tags        space-separaterd list of tag names. Tag names that contain spaces themselfs, should be wrapped with quotes (").
     * @param maxResult   maximum amount of search result hits. This value can not exceed the system wide limit.
     * @param offset      for pagination. Which result 'page' you want.
     */
    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public MediaSearchResult search(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    /**
     * Search within a certain profile. for argument descriptions @see nl.vpro.api.transfer.MediaSearchResult.
     * You can use tags to limit your search.
     *
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     */

    @GET
    @Path("search/{profile}")
    @Produces(MediaType.APPLICATION_JSON)
    public MediaSearchResult searchWithProfile(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);


    /**
     * @since 1.3
     * @param profileName
     * @return
     */
    @GET
    @Path("get/{profile}")
    @Produces(MediaType.APPLICATION_JSON)
    public MediaSearchResultItemIterator getProfile(@PathParam("profile") String profileName);

    /**
     * Find popular search terms for a given prefix. This feature is used for the presentation of suggestions while
     * typing search terms.
     * You can use tags to limit your search.
     *
     * @param termPrefix the prefix terms will be searched for.
     * @param tags       space-separated list of tag names. Tag names that contain spaces themselves, should be wrapped with quotes (").
     */
    @GET
    @Path("search/suggest")
    @Produces(MediaType.APPLICATION_JSON)
    public MediaSearchSuggestions searchSuggestions(@QueryParam("q") String termPrefix, @QueryParam("tags") String tags);

    /**
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     * @see nl.vpro.api.transfer.MediaSearchSuggestions
     *      This version of the call lets you search for popular terms within a certain profile.
     */
    @GET
    @Path("search/suggest/{profile}")
    @Produces(MediaType.APPLICATION_JSON)
    public MediaSearchSuggestions searchSuggestionsWithProfile(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @PathParam("profile") String profileName);

    /**
     * Temporary call added to ease the introduction of Elastic Search support. It allows you to use the full flexibility of the
     * ES query dsl, at the cost of having to use the full flexibility of the ES dsl :-)
     * This method will be replaced by a set of higher level search calls as soon as we know what they should be.
     *
     * @param index What ES index to use?
     * @param query the query, in json format.
     * @param typesAsString space-separaterd list of types, as they occur in the given index. This is actually a query filter.
     * @deprecated This is only a temporary call and will be removed in a future version.
     */
    @POST
    @Path("search/es/{index}")
    @Deprecated
    @Produces(MediaType.APPLICATION_JSON)
    public String searchES(@PathParam("index") String index, @FormParam("query") String query, @FormParam("documentTypes") String typesAsString);
}
