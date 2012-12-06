/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import org.elasticsearch.index.query.BoolQueryBuilder;

import java.util.List;

/**
 * User: rico
 * Date: 05/12/2012
 */
public class ParsingStringConstraintBuilder extends BoolQueryBuilder {

    public ParsingStringConstraintBuilder(List<String> constraints) {
        for (String constraint : constraints) {
            must(ESUtil.parseConstraint(constraint));
        }
    }
}
