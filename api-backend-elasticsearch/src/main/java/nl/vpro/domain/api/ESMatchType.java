/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
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
        public FilterBuilder getFilterBuilder(String fieldName, String value, boolean caseSensitive) {
            return FilterBuilders.termFilter(esField(fieldName, caseSensitive), esValue(value, caseSensitive));
        }

        @Override
        public QueryBuilder getQueryBuilder(String fieldName, String value, boolean caseSensitive) {
            return QueryBuilders.termQuery(esField(fieldName, caseSensitive), esValue(value, caseSensitive));
        }
    },
    REGEX {
        @Override
        public FilterBuilder getFilterBuilder(String fieldName, String value, boolean caseSensitive) {
            return FilterBuilders.regexpFilter(esField(fieldName, caseSensitive), esValue(value, caseSensitive));
        }

        @Override
        public QueryBuilder getQueryBuilder(String fieldName, String value, boolean caseSensitive) {
            return QueryBuilders.regexpQuery(esField(fieldName, caseSensitive), esValue(value, caseSensitive));
        }
    },
    WILDCARD {
        @Override
        public FilterBuilder getFilterBuilder(String fieldName, String value, boolean caseSensitive) {
            String regex = wildcard2regex(value);
            return FilterBuilders.regexpFilter(esField(fieldName, caseSensitive), esValue(regex, caseSensitive));
        }

        @Override
        public QueryBuilder getQueryBuilder(String fieldName, String value, boolean caseSensitive) {
            return QueryBuilders.wildcardQuery(esField(fieldName, caseSensitive), esValue(value, caseSensitive));
        }

    };

    private static final String LOWERCASEFIELD = "lower";

    public abstract FilterBuilder getFilterBuilder(String fieldName, String value, boolean caseSensitive);

    public abstract QueryBuilder getQueryBuilder(String fieldName, String value, boolean caseSensitive);

    protected String esValue(String value, boolean caseSensitive) {
        return caseSensitive ? value : value.toLowerCase();
    }

    public static String esField(String fieldName, boolean caseSensitive) {
        return caseSensitive ? fieldName : fieldName + "." + LOWERCASEFIELD;
    }

}
