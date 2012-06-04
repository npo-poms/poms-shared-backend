package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.searchfilterbuilder.SearchFilter;
import nl.vpro.api.service.searchfilterbuilder.TagFilter;
import nl.vpro.api.util.SolrQueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 15-5-12
 * Time: 11:47
 *
 * @author Ernst Bunders
 */
public abstract class AbstractSolrQueryFactory implements SolrQueryFactory {
    private static final Logger log = LoggerFactory.getLogger(AbstractSolrQueryFactory.class);
    protected SolrQueryBuilder solrQueryBuilder;
    private SolrServer solrServer;

    public void setSolrQueryBuilder(SolrQueryBuilder solrQueryBuilder) {
        this.solrQueryBuilder = solrQueryBuilder;
    }

    public SolrQueryBuilder getSolrQueryBuilder() {
        return solrQueryBuilder;
    }

    public SolrServer getSolrServer() {
        return solrServer;
    }

    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    protected void setFacetFields(String term, Integer minOccurrence, Integer limit, SolrQuery solrQuery) {
        solrQuery.setFacet(true);
        solrQuery.setFacetLimit(limit);
        solrQuery.addFacetField(searchFields.toArray(new String[searchFields.size()]));
        solrQuery.setFacetPrefix(term);
        solrQuery.setFacetMinCount(minOccurrence);
        solrQuery.setFields(searchFields.toArray(new String[searchFields.size()]));
        solrQuery.setRows(0);
    }

    protected String createTermQuery(String term) {
        StringBuilder termQueryBuilder = new StringBuilder();
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

    private String createSearchQuery(String termQuery, String tagsQuery) {
        StringBuilder queryBuilder = new StringBuilder();
        if (StringUtils.isBlank(tagsQuery)) {
            queryBuilder.append(termQuery);
        } else {
            queryBuilder.append("(")
                .append(termQuery)
                .append(") AND (")
                .append(tagsQuery)
                .append(")");
        }
        String searchQuery = queryBuilder.toString().trim();
        log.debug("Search Query: " + searchQuery);
        return searchQuery;
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
                    .append(tag)
                    .append(" ");

            }
        }
        String tagsQuery = tagsBuilder.toString().trim();
        log.debug("Tags Query: " + tagsQuery);
        return tagsQuery;
    }

    public SolrQuery createDefaultLuceneQuery(Profile profile, String term, TagFilter tagFilter, Integer max, Integer offset) {
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

    public SolrQuery createDefaultLuceneSuggestQuery(Profile profile, String term, TagFilter tagFilter, Integer minOccurrence, Integer limit) {
        SolrQuery solrQuery = solrQueryBuilder.build();
        setFilterQuery(profile, solrQuery);
        solrQuery.setQuery(createSearchQuery(createTermQuery(""), createTagsQuery(tagFilter)));
        setFacetFields(term, minOccurrence, limit, solrQuery);
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
}
