package nl.vpro.api.service.search;

import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.filterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.search.filterbuilder.SearchFilter;
import nl.vpro.api.service.search.filterbuilder.TagFilter;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.SearchQuery;
import nl.vpro.api.transfer.SearchSuggestions;
import nl.vpro.api.util.SolrQueryBuilder;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.util.List;

/**
 * This is the solr implementation of the search interface.
 *
 *The tag filter behaviour that we want (tags boost the search result but do not cause search hits)
 * can not be done with the SolrSearch impl.
 * Because the dismax parser does not support AND and OR syntax in the query, we can not use it as it does not allow
 * us to build the filter queryies.
 * But dismax supports the 'bq' (boost query) param, and the lucene query parser does not. Catch 22!
 *
 * We support the use case where tags boost the search result, but will also create hits. Not nice, let's move to ES
 * @author Ernst Bunders
 */
public class SolrSearch extends AbstractSearch {

    private static final Logger log = LoggerFactory.getLogger(SolrSearch.class);

    @Autowired
    private SolrServer solrServer;

    @Autowired
    private SolrQueryBuilder solrQueryBuilder;

    @Autowired
    ConversionService conversionService;

    @Override
    public MediaSearchResult search(Profile profile, String term, TagFilter tagFilter, Integer offset, Integer maxResult) {
        SolrQuery solrQuery = createQuery(profile, term, tagFilter, maxResult, offset);
        if (log.isDebugEnabled()) {
            log.debug("Server: " + ((HttpSolrServer) solrServer).getBaseURL().toString());
            log.debug("Query: " + solrQuery.toString());
        }
        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchResult.class);
        } catch (SolrServerException e) {
            throw new ServerErrorException("Something went wrong submitting search query to solr: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchSuggestions suggest(Profile profile, String term, TagFilter tagFilter, List<String> constraints, Integer minOccurrence, Integer limit) {
        SolrQuery solrQuery = solrQueryBuilder.build();
        setFilterQuery(profile, solrQuery);
        solrQuery.setQuery(createSearchQuery(createTermQuery(""), createTagsQuery(tagFilter)));
        setFacetFields(term, minOccurrence, limit, solrQuery);
        QueryResponse response = null;

        if (constraints!=null && constraints.size()>0) {
            log.warn("Contraints are set, but will not be handled by this implementation");
        }
        try {
            response = solrServer.query(solrQuery);
            return conversionService.convert(response, SearchSuggestions.class);
        } catch (SolrServerException e) {
            throw new ServerErrorException("Something went wrong submitting search suggestions query to solr: " + e.getMessage(), e);
        }
    }

    protected void setFacetFields(String term, Integer minOccurrence, Integer limit, SolrQuery solrQuery) {
        solrQuery.setFacet(true);
        solrQuery.setFacetLimit(limit == null ? -1: limit);
        solrQuery.addFacetField(getSearchFields().toArray(new String[getSearchFields().size()]));
        solrQuery.setFacetPrefix(term);
        solrQuery.setFacetMinCount(minOccurrence);
        solrQuery.setFields(getSearchFields().toArray(new String[getSearchFields().size()]));
        solrQuery.setRows(0);
    }

    /**
     * @throws RuntimeException when something goes wrong.
     * @param archiveName
     * @return
     */
    @Override
    public String findArchiveId(String archiveName) throws ServerErrorException{
        log.debug("Profile not found in cache. look up in solr");
        String queryString = new DocumentSearchFilter()
                .addMediaType(MediaType.ARCHIVE)
                .setMainTitle(archiveName)
                .createQueryString();
        SolrQuery query = solrQueryBuilder.build(queryString);

        try {
            QueryResponse response = solrServer.query(query);
            if (response.getResults().getNumFound() == 1) {
                return (String) response.getResults().get(0).getFieldValue("urn");
            } else {
                throw new ServerErrorException("Can not find archive with name " + archiveName + " in Solr. Because number of results for " + archiveName + " is not 1, but  " + response.getResults().getNumFound());
            }
        } catch (SolrServerException e) {
            throw new ServerErrorException("Something went wrong connecting to Solr service.", e);
        }
    }


    private SolrQuery createQuery(Profile profile, String term, TagFilter tagFilter, Integer max, Integer offset) {
        SolrQuery solrQuery = solrQueryBuilder.build();
        setFilterQuery(profile, solrQuery);
        solrQuery.setFields("*", "score");

        String termQuery = createTermQuery(term);
        String tagsQuery = createTagsQuery(tagFilter);
        solrQuery.setQuery(createSearchQuery(termQuery, tagsQuery));

        solrQuery.setRows(max);

        if (offset != null && offset > 0) {
            solrQuery.setStart(offset);
        }
        return solrQuery;
    }

    private void setFilterQuery(Profile profile, SolrQuery solrQuery) {
        SearchFilter filterQuery = profile.createFilterQuery();
        if (filterQuery != null) {
            String filterQueryString = filterQuery.createQueryString();
            if (StringUtils.isNotBlank(filterQueryString)) {
                log.debug("Filter Query: " + filterQueryString);
                solrQuery.setFilterQueries(filterQueryString);
            }
        }
    }

    protected String createTermQuery(String term) {
        StringBuilder termQueryBuilder = new StringBuilder();
        List<String> searchFields = getSearchFields();
        List<Float> searchFieldBoosting = getSearchFieldBoosting();
        if (StringUtils.isBlank(term)) {
            termQueryBuilder
                    .append(searchFields.get(0))
                    .append(":*");
        } else {
            for (int i = 0; i < searchFields.size(); i++) {
                termQueryBuilder.append(searchFields.get(i))
                        .append(":")
                        .append(term)
                        .append("^")
                        .append(searchFieldBoosting.get(i))
                        .append(" ");
            }
        }
        String termQuery = termQueryBuilder.toString().trim();
        log.debug("Term Query: " + termQuery);
        return termQuery;
    }

    private String createTagsQuery(TagFilter tagFilter) {
        StringBuilder tagsBuilder = new StringBuilder();
        if (tagFilter != null && tagFilter.hasTags()) {
            int counter = 0;
            for (String tag : tagFilter.getTags()) {
                if (counter++ > 0) {
                    tagsBuilder
                            .append(tagFilter.getBooleanOp().toString())
                            .append(" ");
                }
                tagsBuilder
                        .append("tag:")
                        .append(asLuceneQueryPhrase(tag))
                        .append(" ");

            }
        }
        String tagsQuery = tagsBuilder.toString().trim();
        log.debug("Tags Query: " + tagsQuery);
        return tagsQuery;
    }

    /**
     * We can not implement this the way we want. Look at the class documentation above.
     * @param termQuery
     * @param tagsQuery
     * @return
     */
    private String createSearchQuery(String termQuery, String tagsQuery) {
        StringBuilder queryBuilder = new StringBuilder();
        if (StringUtils.isBlank(tagsQuery)) {
            queryBuilder.append(termQuery);
        } else {
            queryBuilder
                    .append(termQuery)
                    .append(" ")
                    .append(tagsQuery);
        }
        String searchQuery = queryBuilder.toString().trim();
        log.debug("Search Query: " + searchQuery);
        return searchQuery;
    }

    /**
     * If the input string contains space(s), this method returns the input string between double quotes.
     * If the input contains no space(s), it returns the original input string.
     * Useful for Lucene Queries, see http://www.solrtutorial.com/solr-query-syntax.html for the lucent syntax.
     *
     * @param aString
     */
    private static String asLuceneQueryPhrase(final String aString) {
        String lqp = aString;
        if (StringUtils.containsAny(aString, " ")) {
            lqp = "\"" + aString + "\"";
        }
        return lqp;
    }

    @Override
    public GenericSearchResult search(Profile profile, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public GenericSearchResult search(Profile profile, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) throws ServerErrorException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public GenericSearchResult search(Profile profile, SearchQuery searchQuery) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
