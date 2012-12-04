package nl.vpro.api.service.search;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.es.ProfileFilterBuilder;
import nl.vpro.api.service.search.es.SearchFieldsQueryBuilder;
import nl.vpro.api.service.search.es.SearchResponseExtender;
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
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.parboiled.common.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


/**
 * An ElasticSearch implementation of the Search interface.
 * User: ernst
 * Date: 10/2/12
 * Time: 10:56 AM
 */
public class ESSearch extends AbstractSearch {

    private List<String> searchVPROFields = Arrays.asList("title", "subtitle", "summary", "body", "persons", "genre", "keywords");
    private List<Float> searchVPROBoosting = Arrays.asList(2.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private SearchSourceBuilder createBasicQuery(String term, TagFilter tagFilter, Profile profile, Integer offset, Integer maxResult) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile != null && profile != Profile.DEFAULT && profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        SearchFieldsQueryBuilder searchFieldsQueryBuilder = new SearchFieldsQueryBuilder(getSearchFields(), getSearchFieldBoosting(), term);
        // handle the tags
        if (tagFilter.hasTags()) {
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

    //TODO implement
    @Override
    public String findArchiveId(String archiveName) throws ServerErrorException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MediaSearchSuggestions suggest(Profile profile, String queryString, List<String> constraints, Integer minOccurrence, Integer Limit) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = new SearchRequest(profile.getIndexName());
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile != null && profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }

        searchBuilder.facet(FacetBuilders.termsFacet("suggestions").fields(searchVPROFields.toArray(new String[searchVPROFields.size()])).size(Limit).regex("^" + Pattern.quote(queryString) + ".*"));

        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();

        int boostPos=0;
        for (String field : searchVPROFields) {
            queryBuilder.should(prefixQuery(field,queryString).boost(searchVPROBoosting.get(boostPos)));
            boostPos++;
        }
        for (String stringConstraint : constraints) {
            String field, value;
            int idx = stringConstraint.indexOf(':');
            if (idx > 0) {
                field = stringConstraint.substring(0, idx);
                value = stringConstraint.substring(idx + 1);
                queryBuilder.must(termQuery(field, value));
            }
        }
        searchBuilder.query(queryBuilder);

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture = esClient.search(request);
        try {
            SearchResponse response = searchResponseFuture.actionGet(10, TimeUnit.SECONDS);
            return conversionService.convert(new SearchResponseExtender(response, 0), MediaSearchSuggestions.class);
        } catch (Throwable e) {
            throw new ServerErrorException("Something went wrong performing an elastic search operation:" + e.getMessage(), e);
        }
    }

    @Override
    public GenericSearchResult search(Profile profile, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        if (StringUtils.isEmpty(profile.getIndexName())) {
            throw new ServerErrorException("No index available for the profile " + profile.getName());
        }
        SearchRequest request = new SearchRequest(profile.getIndexName());
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // handle the profile
        if (profile != null && profile.createFilterQuery() != null) {
            searchBuilder.filter(new ProfileFilterBuilder(profile));
        }
//        request.types("poms");
        for (String stringFacet : facets) {
            searchBuilder.facet(FacetBuilders.termsFacet(stringFacet).field(stringFacet).size(10));
        }


        SearchFieldsQueryBuilder queryBuilder = new SearchFieldsQueryBuilder(searchVPROFields, searchVPROBoosting, queryString);
        for (String stringConstraint : constraints) {
            String field, value;
            int idx = stringConstraint.indexOf(':');
            if (idx > 0) {
                field = stringConstraint.substring(0, idx);
                value = stringConstraint.substring(idx + 1);
                queryBuilder.must(termQuery(field, value));
            }
        }

        if (sortFields != null && sortFields.size() > 0) {
            for (String sortField : sortFields) {
                String field, order;
                String[] keys = sortField.trim().split("[: ]+");
                if (keys.length > 0) {
                    if (keys.length == 1) {
                        field = sortField;
                        order = "ASC";
                    } else {
                        field = keys[0];
                        order = keys[1].toUpperCase();
                    }
                    searchBuilder.sort(field, SortOrder.valueOf(order));
                }
            }
        } else {
            searchBuilder.sort(SortBuilders.scoreSort());
        }

        searchBuilder.query(queryBuilder);
        //handle paging
        if (offset != null) {
            searchBuilder.from(offset);
        }
        searchBuilder.size(maxResult);

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture = esClient.search(request);
        try {
            SearchResponse response = searchResponseFuture.actionGet(10, TimeUnit.SECONDS);
            return conversionService.convert(new SearchResponseExtender(response, offset), GenericSearchResult.class);
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
