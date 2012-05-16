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
public interface SolrQueryBuilder {

    public SolrQuery build();

    public SolrQuery build(String query);
}
