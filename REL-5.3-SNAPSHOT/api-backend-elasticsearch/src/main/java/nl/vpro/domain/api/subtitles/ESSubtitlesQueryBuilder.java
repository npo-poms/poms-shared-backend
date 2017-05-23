/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.subtitles;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.ESQueryBuilder;
import nl.vpro.domain.api.SearchFieldDefinition;
import nl.vpro.domain.api.SimpleTextMatcher;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public class ESSubtitlesQueryBuilder extends ESQueryBuilder {

    public static final List<SearchFieldDefinition> SEARCH_FIELDS = Arrays.asList(
        new SearchFieldDefinition("content", 2f)
    );


    public static QueryBuilder query(SubtitlesSearch searches) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        return query(searches, QueryBuilders.boolQuery(), "");
    }

    /**
     * Builds a media relationQuery for standalone or embedded media when the prefix argument is left blank.
     *
     * @param searches
     * @param prefix   not null path to the media node in the documents to search, including the last dot, can be blank
     * @return
     */
    public static QueryBuilder query(SubtitlesSearch searches, @NotNull BoolQueryBuilder booleanQuery, @NotNull String prefix) {
        if(searches == null) {
            return booleanQuery;
        }

        {
            SimpleTextMatcher textSearch = searches.getText();
            if(textSearch != null && StringUtils.isNotBlank(textSearch.getValue())) {
                QueryBuilder textQuery = buildTextQuery(
                    textSearch,
                    prefix,
                    SEARCH_FIELDS
                );
                apply(booleanQuery, textQuery, textSearch.getMatch());
            }
        }

        build(booleanQuery, searches.getMediaIds(), new SingleFieldApplier(prefix + "parent"));
        build(booleanQuery, searches.getTypes(), new SingleFieldApplier(prefix + "type"));
        build(booleanQuery, searches.getLanguages(), new SingleFieldApplier(prefix + "language"));


        if(booleanQuery.hasClauses()) {
            return booleanQuery;
        }

        return QueryBuilders.matchAllQuery();
    }


}
