/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import org.apache.lucene.queryParser.QueryParser;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.util.List;

/**
 * User: rico
 * Date: 05/12/2012
 */
public class MultiQueryStringConstraintBuilder extends BoolQueryBuilder {

    public MultiQueryStringConstraintBuilder(List<String> constraints) {
        for (String constraint : constraints) {
            must(new QueryStringQueryBuilder(constraint));
        }
    }
}
