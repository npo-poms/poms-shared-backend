/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.util;

import org.apache.solr.client.solrj.SolrQuery;

/**
 * User: rico
 * Date: 07/05/2012
 */
public class SolrQueryBuilderImpl implements SolrQueryBuilder {

    public SolrQuery build() {
        SolrQuery query = new SolrQuery();
        query.set("defType", "lucene");
        return query;
    }

    public SolrQuery build(String query) {
        return build().setQuery(query);
    }
}
