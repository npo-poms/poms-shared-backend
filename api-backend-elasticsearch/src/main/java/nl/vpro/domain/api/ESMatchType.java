/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author rico
 * @since 4.6
 */
public enum ESMatchType {
    TEXT {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, FieldInfo fieldInfo) {
            return QueryBuilders.termQuery(esField, value);
        }
    },
    REGEX {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, FieldInfo fieldInfo) {
            // TODO
            return QueryBuilders.regexpQuery(esField, value);
        }
    },
    WILDCARD {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, FieldInfo fieldInfo) {
            return QueryBuilders.wildcardQuery(esField, value);
        }

    };

    public abstract QueryBuilder getQueryBuilder(
        String fieldName, String value, FieldInfo fieldInfo);



    public static String esValue(String value, boolean caseSensitive) {
        return caseSensitive ? value : value.toLowerCase();
    }

    @Data
    @AllArgsConstructor
    @lombok.Builder(builderClassName = "Builder")
    public static class FieldInfo {
        public static class Builder {
            public <T extends Enum<T>> Builder enumValue(Class<T> enumClass) {
                return possibleValues(Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList()));
            }
        }
        public static <T extends Enum<T>> FieldInfo enumValue(Class<T> enumClass) {
            return FieldInfo.builder().enumValue(enumClass).build();
        }
        public static final FieldInfo TEXT = new FieldInfo();
        protected FieldInfo() {

        }
        List<String> possibleValues;
    }

}
