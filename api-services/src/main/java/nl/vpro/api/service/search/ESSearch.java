package nl.vpro.api.service.search;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.es.ProfileFilterBuilder;
import nl.vpro.api.service.search.es.SearchFieldsQueryBuilder;
import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.util.rs.error.ServerErrorException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.util.concurrent.TimeUnit;

/**
 * An ElasticSearch implementation of the Search interface.
 * User: ernst
 * Date: 10/2/12
 * Time: 10:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class ESSearch extends AbstractSearch {

    private String pomsIndexName;

    @Autowired
    private Client esClient;

    @Autowired
    ConversionService conversionService;

    @Override
    public MediaSearchResult search(Profile profile, String term, TagFilter tagFilter, Integer offset, Integer maxResult) throws ServerErrorException {
        SearchSourceBuilder searchBuilder = createBasicQuery(term, tagFilter, profile, offset, maxResult);
        SearchRequest request = new SearchRequest(pomsIndexName);
        request.types("poms");

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture = esClient.search(request);
        try {
            SearchResponse response = searchResponseFuture.actionGet(5, TimeUnit.SECONDS);

            return conversionService.convert(new SearchResponseExtender(response, offset), MediaSearchResult.class);
        } catch (Throwable e) {
            throw new ServerErrorException("Something went wrong performing an elastic search operation:" + e.getMessage(), e);
        }
    }

    //TODO implement
    @Override
    public MediaSearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, Integer minOccurrence, Integer limit) throws ServerErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private SearchSourceBuilder createBasicQuery(String term, TagFilter tagFilter, Profile profile, Integer offset, Integer maxResult) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile != null && profile != Profile.DEFAULT && profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }
        //handle the tags
        //handle paging
        if (offset != null) {
            searchBuilder.from(offset);
        }
        searchBuilder.size(maxResult);

        //handle search fields and boosting
        searchBuilder.query(new SearchFieldsQueryBuilder(getSearchFields(), getSearchFieldBoosting(), term));
        return searchBuilder;
    }

    //TODO implement
    @Override
    public String findArchiveId(String archiveName) throws ServerErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPomsIndexName() {
        return pomsIndexName;
    }

    public void setPomsIndexName(String pomsIndexName) {
        this.pomsIndexName = pomsIndexName;
    }
}
