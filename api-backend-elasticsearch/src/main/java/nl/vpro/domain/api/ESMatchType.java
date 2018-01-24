/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.XmlValued;

/**
 * @author rico
 * @since 4.6
 */
@Slf4j
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
            if (fieldInfo.getCardinality().isPresent()) {
                Pattern pattern = Pattern.compile(value);
                return useCardinality(esField, (s) -> pattern.matcher(s).matches(), fieldInfo.getPossibleValues());
            } else {
                return QueryBuilders.regexpQuery(esField, value);
            }
        }
    },
    WILDCARD {
        @Override
        public QueryBuilder getQueryBuilder(String esField, String value, FieldInfo fieldInfo) {
            if (fieldInfo.getCardinality().isPresent()) {
                Predicate<String> pred = (s) -> FilenameUtils.wildcardMatch(s, value);
                return useCardinality(esField, pred, fieldInfo.getPossibleValues());
            } else {
                if (value.startsWith("?") || value.startsWith("*")) {
                    log.warn("Wildcard query starting with " + value.charAt(0) + ": " + value);
                }

                return QueryBuilders.wildcardQuery(esField, value);
            }
        }

    };

    protected QueryBuilder useCardinality(String esField, Predicate<String> value, List<String> possibleValues) {
        List<QueryBuilder> builders = new ArrayList<>();
        for (String possibleValue : possibleValues) {
            if (value.test(possibleValue)) {
                builders.add(QueryBuilders.termQuery(esField, possibleValue));
            }
        }
        if (builders.size() == possibleValues.size()) {
            // every possible value matches, so this simply means that the value should exist
            return QueryBuilders.existsQuery(esField);
        } else if (builders.size() == 1) {
            return builders.get(0);
        } else if (builders.isEmpty()) {
            return QueryBuilders.termQuery(esField, "____IMPOSSIBLE_VALUES___");
        } else {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            for (QueryBuilder qb : builders) {
                boolQueryBuilder.should(qb);
            }
            return boolQueryBuilder;
        }

    }

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
                return possibleValues(Arrays.stream(enumClass.getEnumConstants())
                    .map(this::xmlValue)
                    .collect(Collectors.toList()));
            }
            protected String xmlValue(Enum<?> enumValue) {
                if (enumValue instanceof XmlValued) {
                    return ((XmlValued)enumValue).getXmlValue();
                } else {
                    return enumValue.name();
                }
            }
        }
        public static <T extends Enum<T>> FieldInfo enumValue(Class<T> enumClass) {
            return FieldInfo.builder().enumValue(enumClass).build();
        }
        public static final FieldInfo TEXT = new FieldInfo();
        protected FieldInfo() {

        }
        List<String> possibleValues;

        public Optional<Integer> getCardinality() {
            return possibleValues == null || possibleValues.isEmpty() ? Optional.empty() : Optional.of(possibleValues.size());
        }

    }

}
