/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.ESFilterBuilder;
import nl.vpro.domain.api.ESQueryBuilder;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.support.Workflow;

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


    public static <T, W extends Enum<W>> QueryBuilder filter(ProfileDefinition<T> profileDefinition) {
        return filter(profileDefinition, "workflow", Workflow.PUBLISHED);
    }

    public static <T, W extends Enum<W>> void filter(
        @Nullable ProfileDefinition<T> profileDefinition,
        @NonNull BoolQueryBuilder rootQuery
        ) {
         filter(profileDefinition, rootQuery, "workflow", Workflow.PUBLISHED);
     }

    /**
     *
     */
    @Deprecated

    public static void filter(String prefix, MediaSearch searches, BoolQueryBuilder filter) {
        ESMediaQueryBuilder.buildMediaQuery(prefix, filter, searches);
    }

     public static QueryBuilder filter(
         @NonNull String prefix,
         @Nullable MediaSearch searches) {
        if (searches == null) {
            return QueryBuilders.matchAllQuery();
        }
        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.buildMediaQuery(prefix, booleanFilter, searches);
        return ESQueryBuilder.simplifyQuery(booleanFilter);

    }


      public static QueryBuilder filterRelationsNested(
          @NonNull String prefix,
          @Nullable RelationSearch relationSearch) {
          if (relationSearch == null) {
              return QueryBuilders.matchAllQuery();
          }

          BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
          ESQueryBuilder.relationNestedQuery(prefix, relationSearch, booleanFilter);
          return ESQueryBuilder.simplifyQuery(booleanFilter);
      }

       public static QueryBuilder filterRelations(
          @NonNull String prefix,
          @Nullable RelationSearch relationSearch) {
          if (relationSearch == null) {
              return QueryBuilders.matchAllQuery();
          }

          BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
          ESQueryBuilder.relationQuery(prefix, relationSearch, boolQueryBuilder);
          return ESQueryBuilder.simplifyQuery(boolQueryBuilder);
      }


}
