/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import static nl.vpro.domain.api.ESUtils.wildcard2regex;

/**
 * @author rico
 * @since 4.6
 */
public enum ESMatchType {
    TEXT {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, boolean caseSensitive) {
            return QueryBuilders.termQuery(esField, esValue(value, caseSensitive));
        }
    },
    REGEX {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, boolean caseSensitive) {
            return QueryBuilders.regexpQuery(esField, esValue(value, caseSensitive));
        }
    },
    WILDCARD {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, boolean caseSensitive) {
            return QueryBuilders.wildcardQuery(esField, esValue(value, caseSensitive));
        }

    };

    public abstract QueryBuilder getQueryBuilder(String fieldName, String value, boolean caseSensitive);

    protected static String esValue(String value, boolean caseSensitive) {
        return caseSensitive ? value : value.toLowerCase();
    }

}
