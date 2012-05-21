package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.searchfilterbuilder.SearchFilter;
import nl.vpro.api.util.SolrQueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;

/**
 * Date: 15-5-12
 * Time: 11:47
 *
 * @author Ernst Bunders
 */
public abstract class AbstractSolrQueryFactory implements SolrQueryFactory {
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

    protected String createQuery(String term) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchFields.size(); i++) {
            sb.append(searchFields.get(i))
                .append(":")
                .append(term)
                .append("^")
                .append(searchFieldBoosting.get(i))
                .append(" ");
        }
        return sb.toString().trim();
    }

    public SolrQuery createDefaultLuceneQuery(Profile profile, String term, Integer max, Integer offset) {
        SolrQuery solrQuery = solrQueryBuilder.build();

        SearchFilter filterQuery = profile.createFilterQuery();
        if (filterQuery != null) {
            String filterQueryString = filterQuery.createQueryString();
            if (StringUtils.isNotBlank(filterQueryString)) {
                solrQuery.setFilterQueries(filterQueryString);
            }
        }

        solrQuery.setFields("*", "score");
        solrQuery.setQuery(createQuery(term));
        solrQuery.setRows(max);

        if (offset != null && offset > 0) {
            solrQuery.setStart(offset);
        }
        return solrQuery;
    }

    public SolrQuery createDefaultLuceneSuggestQuery(Profile profile, String term, Integer minOccurrence, Integer limit) {
        SearchFilter filterQuery = profile.createFilterQuery();
        String filterQueryString = filterQuery.createQueryString();

        SolrQuery solrQuery = solrQueryBuilder.build();
        solrQuery.setQuery("*:*");
        if (StringUtils.isNotBlank(filterQueryString)) {
            solrQuery.setFilterQueries(filterQueryString);
        }
        setFacetFields(term, minOccurrence, limit, solrQuery);

        return solrQuery;
    }
}
