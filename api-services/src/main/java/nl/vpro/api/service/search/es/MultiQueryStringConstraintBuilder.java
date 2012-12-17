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
            int colon = constraint.indexOf(':');
            if (colon > -1) {
                String field = constraint.substring(0, colon + 1);
                String constr = constraint.substring(colon + 1);
                constraint = field + QueryParser.escape(constr);
            }
            must(new QueryStringQueryBuilder(constraint));
        }
    }
}
