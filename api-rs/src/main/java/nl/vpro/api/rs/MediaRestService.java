package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;
import org.elasticsearch.action.search.SearchResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * Public REST API to api.service.MediaService
 *
 * @author : maarten hogendoorn
 */
public interface MediaRestService {
    public Program getProgram(String urn);

    public ProgramList getRecentReplayablePrograms(Integer maxResult, Integer offset, String avType);

    public Annotations getAnnotationsForProgram(String urn);

    /**
     * if addMembers=true, members are added.
     * You can filter the types of members that may be returned, by added a mediaTypes request parameter, which contains comma-seperated list of allowed mediatypes.
     * The values of the media types must be the toString values of class MediaObjectType,
     * No filtering (thus getting all members, regardless its type) is obtained by either:
     * - memberTypesFilter = null or ""
     * - memberTypesFilter = "program,group,segment" (a comma seperated list of all possible mediaObjectTypes)
     */
    public Group getGroup(String urn, boolean addMembers, String memberTypesFilter) throws ServerErrorException, NotFoundException;


    public Segment getSegment(String urn) throws ServerErrorException, NotFoundException;

    /**
     * Search for a certain term.
     * You can use tags to limit your search.
     *
     * @param queryString term to search on.
     * @param tags        space-separaterd list of tag names. Tag names that contain spaces themselfs, should be wrapped with quotes (").
     * @param maxResult   maximum amount of search result hits. This value can not exceed the system wide limit.
     * @param offset      for pagination. Which result 'page' you want.
     */
    public MediaSearchResult search(String queryString, String tags, Integer offset, Integer maxResult);

    /**
     * Search within a certain profile. for argument descriptions @see nl.vpro.api.transfer.MediaSearchResult.
     * You can use tags to limit your search.
     *
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     */
    public MediaSearchResult searchWithProfile(String profileName, String queryString, String tags, Integer maxResult, Integer offset);

    /**
     * Find popular search terms for a given prefix. This feature is used for the presentation of suggestions while
     * typing search terms.
     * You can use tags to limit your search.
     *
     * @param termPrefix the prefix terms will be searched for.
     * @param tags space-separated list of tag names. Tag names that contain spaces themselves, should be wrapped with quotes (").
     */
    public MediaSearchSuggestions searchSuggestions(String termPrefix, String tags);

    /**
     * @see nl.vpro.api.transfer.MediaSearchSuggestions
     * This version of the call lets you search for popular terms within a certain profile.
     * @param profileName when empty defaults to 'no profile'. @see nl.vpro.api.service.Profile for valid values.
     */
    public MediaSearchSuggestions searchSuggestionsWithProfile(String queryString, String tags, String profileName);

    /**
     * Temporary call added to ease the introduction of Elastic Search support. It allows you to use the full flexibility of the
     * ES query dsl, at the cost of having to use the full flexibility of the ES dsl :-)
     * This method will be replaced by a set of higher level search calls as soon as we know what they should be.
     * @param index What ES index to use?
     * @param query the query, in json format.
     * @param types space-separaterd list of types, as they occur in the given index. This is actually a query filter.
     */
    public String searchES(String index, String query, String types);
}
