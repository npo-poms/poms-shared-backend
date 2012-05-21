package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.service.Profile;
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
        return createDefaultLuceneQuery(profile, term, max, offset);
    }


    @Override
    public SolrQuery createSuggestQuery(Profile profile, String term, Integer minOccurrence, Integer limit) {
        return createDefaultLuceneSuggestQuery(profile, term, minOccurrence, limit);
    }
}
