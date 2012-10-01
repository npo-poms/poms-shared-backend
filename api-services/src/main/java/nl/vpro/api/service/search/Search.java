package nl.vpro.api.service.search;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.util.rs.error.ServerErrorException;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 9/26/12
 * Time: 4:35 PM
 *
 * this class abstracts the building of search queries, so we can have different implementations (i.e. Solr and ElasticSearch)
 */
public interface Search {

    /**
     * Search for a certain term.
     * @param profile What profile to use? profiles are a way to add preconfigured filter queries to the search query.
     * @param term
     * @param tagFilter For constraining the search on documents with certain tags.
     * @param offset For paging: how many results to skip?
     * @param maxResult For paging: how long is your search page?
     * @return
     * @throws ServerErrorException that will be handeled  by {@see nl.vpro.util.rs.error.ServerErrorExceptionMapper}
     */
    MediaSearchResult search(Profile profile, String term, TagFilter tagFilter, Integer offset, Integer maxResult) throws ServerErrorException;

    /**
     * Suggests a search term for a given prefix.
     * @param profile
     * @param term the search term prefix
     * @param tagFilter for constraining the search on documents with certain tags.
     * @param minOccurrence only suggest terms that have n matches to 'term'. Defaults to 1
     * @param limit only suggest terms that have at most n matches to 'term'. defaults to 'no limit'
     * @return
     * @throws ServerErrorException
     */
    MediaSearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, Integer minOccurrence, Integer limit) throws ServerErrorException;

    /**
     * Some profiles use a (poms) archive as constraint (i.e. all content belonging to..)
     * The implementation of this interface knows how to find a group by it's name.
     * @param archiveName
     * @return
     */

    String findArchiveId(String archiveName) throws ServerErrorException;

}
