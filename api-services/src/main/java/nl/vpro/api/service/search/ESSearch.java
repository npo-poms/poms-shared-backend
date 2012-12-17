package nl.vpro.api.service.search;

import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.es.*;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.util.rs.error.ServerErrorException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.parboiled.common.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


/**
 * An ElasticSearch implementation of the Search interface.
 * User: ernst
 * Date: 10/2/12
 * Time: 10:56 AM
 */
public class ESSearch extends AbstractSearch {

    private String pomsIndexName;

    @Autowired
    private Client esClient;

    @Autowired
    ConversionService conversionService;

    @Value("${elasticSearch.facet.limit}")
    private Integer facetLimit;

    @Value("${elasticSearch.timeout}")
    private Integer timeoutInSeconds;

    @Override
    public MediaSearchResult search(Profile profile, String term, TagFilter tagFilter, Integer offset, Integer maxResult) throws ServerErrorException {
        SearchSourceBuilder searchBuilder = createBasicQuery(term, tagFilter, profile, offset, maxResult);
        SearchRequest request = new SearchRequest(pomsIndexName);
        request.types("poms");

        request.source(searchBuilder);

        if (offset == null) {
            offset = 0;
        }

        return executeQuery(request, offset, MediaSearchResult.class);
    }

    @Override
    public MediaSearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, Integer minOccurrence, Integer limit) throws ServerErrorException {
        SearchRequest request = new SearchRequest(pomsIndexName);
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        // handle the profile
        if (profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        searchBuilder.facet(new SuggestionFacetBuilder(profile, "suggestions", term, limit));

        SearchFieldsQueryBuilder queryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), term);
        if (tagFilter!=null && tagFilter.hasTags()) {
            for (String tag : tagFilter.getTags()) {
                queryBuilder.should(termQuery("tags", tag));
            }
        }
        searchBuilder.query(queryBuilder);

        request.types("poms");
        request.source(searchBuilder);

        return executeQuery(request, 0, MediaSearchSuggestions.class);
    }

    private SearchSourceBuilder createBasicQuery(String term, TagFilter tagFilter, Profile profile, Integer offset, Integer maxResult) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile != null && profile.createFilterQuery() != null) {

//            searchBuilder.filter(termFilter("broadcasters", "vpro"));
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        SearchFieldsQueryBuilder searchFieldsQueryBuilder = new SearchFieldsQueryBuilder(getSearchFields(), getSearchFieldBoosting(), term);
        // handle the tags
        if (tagFilter!=null && tagFilter.hasTags()) {
            for (String tag : tagFilter.getTags()) {
                searchFieldsQueryBuilder.should(termQuery("tags", tag));
            }
        }

        //handle paging
        if (offset != null) {
            searchBuilder.from(offset);
        }
        searchBuilder.size(maxResult);

        //handle search fields and boosting
        searchBuilder.query(searchFieldsQueryBuilder);
        return searchBuilder;
    }

    @Override
    public String findArchiveId(String archiveName) throws ServerErrorException {
        SearchRequest request = new SearchRequest(pomsIndexName);
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        queryBuilder.must(termQuery("mediaType", MediaType.ARCHIVE.toString().toLowerCase()));
        queryBuilder.must(termQuery("mainTitle", archiveName));

        request.types("poms");
        request.source(searchBuilder);
        MediaSearchResult result = executeQuery(request, 0, MediaSearchResult.class);
        if (result.getNumFound() > 0) {
            return result.getMediaSearchResultItems().get(0).getUrn();
        }
        return null;
    }

    @Override
    public MediaSearchSuggestions suggest(Profile profile, String queryString, List<String> constraints, Integer minOccurrence, Integer Limit) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = new SearchRequest(profile.getIndexName());
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        searchBuilder.facet(new SuggestionFacetBuilder(profile, "suggestions", queryString, Limit));

        SearchFieldsQueryBuilder queryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), queryString);
        if (constraints.size() > 0) {
            queryBuilder.must(new MultiQueryStringConstraintBuilder(constraints));
        }

        searchBuilder.query(queryBuilder);
        request.source(searchBuilder);

        return executeQuery(request, 0, MediaSearchSuggestions.class);
    }

    @Override
    public GenericSearchResult search(Profile profile, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = new SearchRequest(profile.getIndexName());
        SearchSourceBuilder searchBuilder = new OrderedSearchSourceBuilder(sortFields);


        // handle the profile
        if (profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        for (String stringFacet : facets) {
            searchBuilder.facet(FacetBuilders.termsFacet(stringFacet).field(stringFacet).size(facetLimit));
        }

        BoolQueryBuilder queryBuilder;
        if (StringUtils.isNotEmpty(queryString)) {
            queryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), queryString);
        } else {
            queryBuilder = new BoolQueryBuilder();
        }
        if (constraints.size() > 0) {
            queryBuilder.must(new MultiQueryStringConstraintBuilder(constraints));
        }

        searchBuilder.query(queryBuilder);
        //handle paging
        if (offset != null) {
            searchBuilder.from(offset);
        } else {
            offset = 0;
        }
        searchBuilder.size(maxResult);

        request.source(searchBuilder);

        return executeQuery(request, offset, GenericSearchResult.class);
    }

    private <T> T executeQuery(SearchRequest request, int offset, Class<T> targetType) throws ServerErrorException {
        ActionFuture<SearchResponse> searchResponseFuture = esClient.search(request);
        try {
            SearchResponse response = searchResponseFuture.actionGet(timeoutInSeconds, TimeUnit.SECONDS);
            return (T) conversionService.convert(new SearchResponseExtender(response, offset), targetType);
        } catch (Throwable e) {
            throw new ServerErrorException("Something went wrong performing an elastic search operation:" + e.getMessage(), e);
        }
    }

    public String getPomsIndexName() {
        return pomsIndexName;
    }

    public void setPomsIndexName(String pomsIndexName) {
        this.pomsIndexName = pomsIndexName;
    }

}
