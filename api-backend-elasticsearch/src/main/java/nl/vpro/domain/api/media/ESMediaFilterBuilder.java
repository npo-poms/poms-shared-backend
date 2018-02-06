/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.ESFilterBuilder;
import nl.vpro.domain.api.ESQueryBuilder.TextSingleFieldApplier;

import static nl.vpro.domain.api.ESQueryBuilder.simplifyQuery;

/**
 * NOTE!: There is also a{@link ESMediaQueryBuilder} equivalent that more or less contains the same code for
 * building queries.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 * @TODO This can be largely dropped in favour of {@link ESMediaQueryBuilder}
 *
 */
public class ESMediaFilterBuilder extends ESFilterBuilder {

    public static QueryBuilder filter(MemberRefSearch searches, @Nonnull String axis) {
        return filter("", axis, searches);
    }

    /**
     *
     * @param axis Non empty string
     */
    public static QueryBuilder filter(
        @Nonnull String prefix,
        @Nonnull String axis,
        MemberRefSearch searches) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        if(searches.getMediaIds() != null && !searches.getMediaIds().isEmpty()) {
            build(prefix, booleanFilter, searches.getMediaIds(), new TextSingleFieldApplier<>(axis + ".midRef"));
        }

        if(searches.getTypes() != null && !searches.getTypes().isEmpty()) {
            build(prefix, booleanFilter, searches.getTypes(), new TextSingleFieldApplier<>(axis + ".type"));
        }
        return booleanFilter;
    }

    public static void filter(
        @Nonnull String axis,
        RelationSearch searches,
        BoolQueryBuilder boolQueryBuilder) {
        filter("", axis, searches, boolQueryBuilder);
    }

    public static void filter(
        @Nonnull String prefix,
        String axis,
        RelationSearch relationSearch,
        BoolQueryBuilder boolQueryBuilder) {
        if(relationSearch == null) {
            return;
        }
        ESMediaQueryBuilder.relationQuery(prefix + axis, relationSearch, boolQueryBuilder);
    }

    public static QueryBuilder filter(@Nonnull String axis, TitleSearch searches) {
        return filter("", axis, searches);
    }

    public static QueryBuilder filter(@Nonnull String prefix, String axis, TitleSearch titleSearch) {
        if(titleSearch == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.buildTitleQuery(prefix, booleanFilter, titleSearch);
        return simplifyQuery(booleanFilter);
    }



    public static QueryBuilder filter(MediaSearch searches) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.query("", searches, boolQueryBuilder);
        return simplifyQuery(boolQueryBuilder);
    }

    public static void filter(String prefix, MediaSearch searches, BoolQueryBuilder filter) {
        ESMediaQueryBuilder.query(prefix, searches, filter);
    }






}
