package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.service.Profile;
import nl.vpro.api.service.searchfilterbuilder.TagFilter;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;

import java.util.Arrays;
import java.util.List;

/**
 * This class was introduced to abstract away the difference between searching through solr or through
 * elasticsearch with the solr plugin
 * Date: 15-5-12
 * Time: 11:35
 *
 * @author Ernst Bunders
 */
public interface SolrQueryFactory {

    static final List<String> searchFields = Arrays.asList("titleMain", "titleAlternative", "descriptionMain", "descriptionShort", "descriptionAlternative");
    static final List<Float> searchFieldBoosting = Arrays.asList(2.0f, 2.0f, 1.0f, 1.0f, 1.0f);


    SolrQuery createSearchQuery(Profile profile, String term, TagFilter tagFilter, Integer max, Integer offset);

    SolrQuery createSuggestQuery(Profile profile, String term, TagFilter tagFilter, Integer minOccurrence, Integer limit);

    SolrServer getSolrServer();
}
