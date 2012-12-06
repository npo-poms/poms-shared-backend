package nl.vpro.api.rs;

import java.util.List;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;

import javax.ws.rs.*;

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
    @Produces("application/json")
    public Program getProgram(@PathParam("urn") String urn);

    @GET
    @Path("program/replay")
    @Produces("application/json")
    public ProgramList getRecentReplayablePrograms(@QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset, @QueryParam("type") String avType);

    @GET
    @Path("program/{urn}/annotations")
    @Produces("application/json")
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

    public Group getGroup(String urn, boolean addMembers, String memberTypesFilter) throws ServerErrorException, NotFoundException;

    @GET
    @Path("group/{urn}")
    @Produces("application/json")
    public Group getGroup(@PathParam("urn") String urn, @QueryParam("members") @DefaultValue("false") boolean addMembers, @QueryParam("episodes") @DefaultValue("false") boolean addEpisodes, @QueryParam("membertypes") String memberTypesFilter) throws ServerErrorException, NotFoundException;

    @GET
    @Path("segment/{urn}")
    @Produces("application/json")
    public Segment getSegment(@PathParam("urn") String urn) throws ServerErrorException, NotFoundException;

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
    @Produces("application/json")
    public MediaSearchResult search(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    /**
     * Search within a certain profile. for argument descriptions @see nl.vpro.api.transfer.MediaSearchResult.
     * You can use tags to limit your search.
     *
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     */

    @GET
    @Path("search/{profile}")
    @Produces("application/json")
    public MediaSearchResult searchWithProfile(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

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
    @Produces("application/json")
    public MediaSearchSuggestions searchSuggestions(@QueryParam("q") String termPrefix, @QueryParam("tags") String tags);

    /**
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     * @see nl.vpro.api.transfer.MediaSearchSuggestions
     *      This version of the call lets you search for popular terms within a certain profile.
     */
    @GET
    @Path("search/suggest/{profile}")
    @Produces("application/json")
    public MediaSearchSuggestions searchSuggestionsWithProfile(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @PathParam("profile") String profileName);

    /**
     * Find related media.
     *
     * @param urn       The urn of the program to find related media for.
     * @param offset    Paging offset.
     * @param maxResult Maximum number of results to return.
     * @return
     */
    @GET
    @Path("related/{urn}")
    @Produces("application/json")
    public List<MediaObject> related(@PathParam("urn") String urn, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

    /**
     * Find related media based on a search profile.
     *
     * @param profile   The name of the search profile to limit the related items to.
     * @param urn       The urn of the program to find related media for.
     * @param offset    Paging offset.
     * @param maxResult Maximum number of results to return.
     * @return
     */
    @GET
    @Path("related/{profile}/{urn}")
    @Produces("application/json")
    public List<MediaObject> relatedWithProfile(@PathParam("profile") String profile, @PathParam("urn") String urn, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult);

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
    @Produces("application/json")
    public String searchES(@PathParam("index") String index, @FormParam("query") String query, @FormParam("documentTypes") String typesAsString);
}
