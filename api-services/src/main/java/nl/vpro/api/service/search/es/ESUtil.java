/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import org.apache.lucene.search.BooleanQuery;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryParser;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * User: rico
 * Date: 05/12/2012
 */
public class ESUtil {

    public static SortBuilder parseOrder(String sortField) {
        String field, order;
        String[] keys = sortField.trim().split("[: ]+");
        if (keys.length > 0) {
            if (keys.length == 1) {
                field = sortField;
                order = "ASC";
            } else {
                field = keys[0];
                order = keys[1].toUpperCase();
            }
            return new FieldSortBuilder(field).order(SortOrder.valueOf(order));
        } else {
            return null;
        }
    }

}
