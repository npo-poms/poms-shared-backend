package nl.vpro.api.service.search;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.SearchQuery;
import nl.vpro.api.transfer.SearchSuggestions;
import nl.vpro.util.rs.error.ServerErrorException;

import java.util.List;

/**
 * this class abstracts the building of search queries, so we can have different implementations (i.e. Solr and ElasticSearch)
 *
 * Tag filter: for now we ignore the boolean op in the tag filter. What we want is just score boosting based on
 * the tags found in the documents. What we dont want is that documents are found by the tags if those are the
 * only matches on a document.
 *
 * Later we want something els: it should be possible to make a distinction between three types of tag filter behaviour:
 * - AND The tags should all be present in the document.
 * - OR At least one of the tags should be in the document
 * - BOOST The above behaviour.
 * We can simply implement it with tag prefixes, like
 * - no prefix=boost
 * - + prefix = and
 * - ~ prefix = or
 * @author Ernst Bunders
 */
public interface Search {

    /**
     * Search for a certain term.
     * @param profile What profile to use? profiles are a way to add pre configured filter queries to the search query.
     * @param term
     * @param tagFilter For constraining the search on documents with certain tags.
     * @param offset For paging: how many results to skip?
     * @param maxResult For paging: how long is your search page?
     * @return
     * @throws ServerErrorException that will be handled  by {@link nl.vpro.util.rs.error.ServerErrorExceptionMapper}
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
    SearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, List<String> constraints, Integer minOccurrence, Integer limit) throws ServerErrorException;

    /**
     * Some profiles use a (poms) archive as constraint (i.e. all content belonging to..)
     * The implementation of this interface knows how to find a group by it's name.
     * @param archiveName the archive we look for
     * @return the id (urn) of given archive, or null when not found
     */

    String findArchiveId(String archiveName) throws ServerErrorException;

    GenericSearchResult search(Profile profile, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException;

    GenericSearchResult search(Profile profile, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException;

    GenericSearchResult search(Profile profile, SearchQuery searchQuery);
}
