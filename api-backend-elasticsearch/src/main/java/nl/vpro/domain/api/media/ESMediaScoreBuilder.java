/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaScoreBuilder {
    static Long sortDateScale = 5 * 365 * 24 * 60 * 60 * 1000L;

    static Long sortDateOffset = 7 * 24 * 60 * 60 * 1000L;

    static double sortDateDecay = 0.6;

    static float locationBoost = 2f;

    static float seriesBoost = 2.5f;

    static float broadcastBoost = 1.5f;

    static float maxBoost = 2.0f;

    public static QueryBuilder score(QueryBuilder query) {
        FunctionScoreQueryBuilder builder = QueryBuilders.functionScoreQuery(query);

        // TODO
        builder
            .scoreMode(FiltersFunctionScoreQuery.ScoreMode.SUM) // Add the individual functions scores (mainly their  boost factors) below
            .maxBoost(maxBoost) // restrict range from 0 to 2
          /*  .boo(QueryBuilders.existsQuery("locations"), ScoreFunctionBuilders.fieldValueFactorFunction(locationBoost))
            .add(QueryBuilders.termQuery("type", MediaType.SERIES.name()), new FactorBuilder().boostFactor(seriesBoost))
            .add(QueryBuilders.termQuery("type", MediaType.BROADCAST.name()), new FactorBuilder().boostFactor(broadcastBoost))
            .add(new GaussDecayFunctionBuilder("sortDate", System.currentTimeMillis(), sortDateScale).setOffset(sortDateOffset).setDecay(sortDateDecay))*/
        ;

        return builder.scoreMode(FiltersFunctionScoreQuery.ScoreMode.MULTIPLY);
    }
}
