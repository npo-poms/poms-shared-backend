package nl.vpro.api.service.search;

import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.es.*;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.SearchQuery;
import nl.vpro.api.transfer.SearchSuggestions;
import nl.vpro.util.Helper;
import nl.vpro.util.rs.error.ServerErrorException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.parboiled.common.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        SearchRequest request = createRequest(profile);

        request.source(searchBuilder);

        return executeQuery(request, offset, MediaSearchResult.class);
    }

    @Override
    public SearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, List<String> constraints, Integer minOccurrence, Integer limit) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = createRequest(profile);
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        ProfileFilterBuilder profileFilterBuilder = null;
        if (profile.createFilterQuery() != null) {
            profileFilterBuilder = new ProfileFilterBuilder(profile);
        }

        searchBuilder.facet(new SuggestionFacetBuilder(profile, "suggestions", term, limit));

        SearchFieldsQueryBuilder queryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), term);

        if (tagFilter != null && tagFilter.hasTags()) {
            for (String tag : tagFilter.getTags()) {
                queryBuilder.should(termQuery("tags", tag));
            }
        }

        if (constraints.size() > 0) {
            queryBuilder.must(new MultiQueryStringConstraintBuilder(constraints));
        }

        searchBuilder.query(new FilteredQueryBuilder(queryBuilder, profileFilterBuilder));
        request.source(searchBuilder);

        return executeQuery(request, 0, SearchSuggestions.class);
    }

    private SearchSourceBuilder createBasicQuery(String term, TagFilter tagFilter, Profile profile, Integer offset, Integer maxResult) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        SearchFieldsQueryBuilder searchFieldsQueryBuilder;

        // handle the profile
        if (profile != null) {
            if (profile.createFilterQuery() != null) {
                searchBuilder.filter(new ProfileFilterBuilder(profile));
            }
            searchFieldsQueryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), term);
        } else {
            searchFieldsQueryBuilder = new SearchFieldsQueryBuilder(Collections.<String>emptyList(), Collections.<Float>emptyList(), term);
        }

        // handle the tags
        if (tagFilter != null && tagFilter.hasTags()) {
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
    public GenericSearchResult search(Profile profile, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = createRequest(profile);
        SearchSourceBuilder searchBuilder = new OrderedSearchSourceBuilder(sortFields);

        // handle the profile
        ProfileFilterBuilder profileFilter = null;
        if (profile.createFilterQuery() != null) {
            profileFilter = new ProfileFilterBuilder(profile);
        }

        QueryBuilder queryBuilder;
        if (constraints.size() > 0) {
            queryBuilder = new MultiQueryStringConstraintBuilder(constraints);
        } else {
            queryBuilder = new MatchAllQueryBuilder();
        }

        searchBuilder.query(new FilteredQueryBuilder(queryBuilder, profileFilter));

        for (String stringFacet : facets) {
            searchBuilder.facet(FacetBuilders.termsFacet(stringFacet).field(stringFacet).size(facetLimit));
        }

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

    @Override
    public GenericSearchResult search(Profile profile, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = createRequest(profile);
        SearchSourceBuilder searchBuilder = new OrderedSearchSourceBuilder(sortFields);

        // handle the profile
        ProfileFilterBuilder profileFilter = null;
        if (profile.createFilterQuery() != null) {
            profileFilter = new ProfileFilterBuilder(profile);
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

        searchBuilder.query(new FilteredQueryBuilder(queryBuilder, profileFilter));

        for (String stringFacet : facets) {
            searchBuilder.facet(FacetBuilders.termsFacet(stringFacet).field(stringFacet).size(facetLimit));
        }

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

    @Override
    public GenericSearchResult search(Profile profile, SearchQuery searchQuery) {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = createRequest(profile);
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        QueryBuilder queryBuilder = null;
        // query
        BoolQueryBuilder boolQueryBuilder;
        if (StringUtils.isNotEmpty(searchQuery.getQuery())) {
            boolQueryBuilder = new SearchFieldsQueryBuilder(profile.getSearchFields(), profile.getSearchBoosting(), searchQuery.getQuery());
        } else {
            boolQueryBuilder = new BoolQueryBuilder();
        }
        // constraints
        for (SearchQuery.Constraint constraint : searchQuery.getConstraints()) {
            boolQueryBuilder.must(termQuery(constraint.getField(), constraint.getValue()));
        }
        if (!boolQueryBuilder.hasClauses()) {
            queryBuilder = new MatchAllQueryBuilder();
        } else {
            queryBuilder = boolQueryBuilder;
        }

        // handle the profile
        ProfileFilterBuilder profileFilterBuilder = null;
        if (profile.createFilterQuery() != null) {
            profileFilterBuilder = new ProfileFilterBuilder(profile);
        }

        // join query and filter
        searchBuilder.query(new FilteredQueryBuilder(queryBuilder, profileFilterBuilder));


        // sort order
        if (searchQuery.getSortOrder().size() > 0) {
            for (SearchQuery.SortField sortField : searchQuery.getSortOrder()) {
                searchBuilder.sort(sortField.getField(), SortOrder.valueOf(sortField.getDirection()));
            }
        } else if (profile.getScoreField() != null) {
            CustomScoreQueryBuilder customScoreQueryBuilder = new CustomScoreQueryBuilder(queryBuilder);
            customScoreQueryBuilder.script(ScoreScriptGenerator.generate(profile.getScoreField(), profile.getScoreTable()));
            queryBuilder = customScoreQueryBuilder;
        }

        // facets
        for (SearchQuery.Facet facet : searchQuery.getFacets()) {
            searchBuilder.facet(FacetBuilders.termsFacet(facet.getField()).field(facet.getField()).size(facet.getNumber()));
        }

        // highlights
        if (searchQuery.getHighlights().size() > 0) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.tagsSchema("styled");
            for (SearchQuery.Highlight highlight : searchQuery.getHighlights()) {
                highlightBuilder.field(highlight.getField(), highlight.getSize(), highlight.getNumber());
            }
            searchBuilder.highlight(highlightBuilder);
        }

        //handle paging
        if (searchQuery.getOffset() != null) {
            searchBuilder.from(searchQuery.getOffset());
        }
        searchBuilder.size(searchQuery.getMax());

        if (Helper.isNotEmpty(searchQuery.getType())) {
            request.types(searchQuery.getType());
        }

        request.source(searchBuilder);

        return executeQuery(request, searchQuery.getOffset(), GenericSearchResult.class);
    }

    private SearchRequest createRequest(Profile profile) {
        if (profile != null && StringUtils.isNotEmpty(profile.getIndexName())) {
            return new SearchRequest(profile.getIndexName());
        } else {
            return new SearchRequest(pomsIndexName);
        }
    }

    private <T> T executeQuery(SearchRequest request, Integer offset, Class<T> targetType) throws ServerErrorException {
        ActionFuture<SearchResponse> searchResponseFuture = esClient.search(request);
        if (offset == null) {
            offset = 0;
        }

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
