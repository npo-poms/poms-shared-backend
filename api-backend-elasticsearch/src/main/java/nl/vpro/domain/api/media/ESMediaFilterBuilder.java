/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.ESFilterBuilder;
import nl.vpro.domain.api.ESQueryBuilder;

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

    /**
     *
     */
    @Deprecated

    public static void filter(String prefix, MediaSearch searches, BoolQueryBuilder filter) {
        ESMediaQueryBuilder.buildMediaQuery(prefix, filter, searches);
    }

     public static QueryBuilder filter(
         @Nonnull String prefix,
         @Nullable MediaSearch searches) {
        if (searches == null) {
            return QueryBuilders.matchAllQuery();
        }
        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.buildMediaQuery(prefix, booleanFilter, searches);
        return ESQueryBuilder.simplifyQuery(booleanFilter);

    }


      public static QueryBuilder filterRelationsNested(
          @Nonnull String prefix,
          @Nullable RelationSearch relationSearch) {
          if (relationSearch == null) {
              return QueryBuilders.matchAllQuery();
          }

          BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
          ESQueryBuilder.relationNestedQuery(prefix, relationSearch, booleanFilter);
          return ESQueryBuilder.simplifyQuery(booleanFilter);
      }

       public static QueryBuilder filterRelations(
          @Nonnull String prefix,
          @Nullable RelationSearch relationSearch) {
          if (relationSearch == null) {
              return QueryBuilders.matchAllQuery();
          }

          BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
          ESQueryBuilder.relationQuery(prefix, relationSearch, boolQueryBuilder);
          return ESQueryBuilder.simplifyQuery(boolQueryBuilder);
      }


}
