package nl.vpro.api.service;

import nl.vpro.api.service.search.SearchResult;

/**
 * Date: 12-3-12
 * Time: 15:45
 *
 * @author Ernst Bunders
 */
public interface SearchService {

    public SearchResult searchMediaWithAncestor(String ancestorUrn, String term, Integer offset, Integer max);

}
