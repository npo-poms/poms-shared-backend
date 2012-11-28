package nl.vpro.api.service.search.es;

import org.elasticsearch.action.search.SearchResponse;

/**
 * This class was introduced to fix the shortcomings of the @link SearchResponse class.
 * User: ernst
 * Date: 10/2/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchResponseExtender{
    private SearchResponse searchResponse;
    private Integer start;

    public SearchResponseExtender(SearchResponse searchResponse, Integer start) {
        this.searchResponse = searchResponse;
        this.start = start;
    }

    public SearchResponse searchResponse() {
        return searchResponse;
    }

    public Integer start() {
        return start;
    }
}
