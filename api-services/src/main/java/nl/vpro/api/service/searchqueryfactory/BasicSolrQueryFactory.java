package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.searchfilterbuilder.SearchFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

/**
 * This class creates the solr queries for the solr search service.
 * Date: 15-5-12
 * Time: 11:37
 *
 * @author Ernst Bunders
 */
public class BasicSolrQueryFactory extends AbstractSolrQueryFactory {
    @Override
    public SolrQuery createSearchQuery(Profile profile, String term, Integer max, Integer offset) {
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

    private String createQuery(String term) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchFields.size(); i++) {
            sb.append(searchFields.get(i))
                .append(":")
                .append(term)
//                .append("^")
//                .append(searchFieldBoosting.get(i))
                .append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public SolrQuery createSuggestQuery(Profile profile, String term, Integer minOccurrence, Integer limit) {
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
