package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.util.SolrQueryBuilder;
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
}
