/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.factor.FactorBuilder;
import org.elasticsearch.index.query.functionscore.gauss.GaussDecayFunctionBuilder;

import nl.vpro.domain.media.MediaType;

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

        builder
            .scoreMode("sum") // Add the individual functions scores (mainly their  boost factors) below
            .maxBoost(maxBoost) // restrict range from 0 to 2
            .add(FilterBuilders.existsFilter("locations"), new FactorBuilder().boostFactor(locationBoost))
            .add(FilterBuilders.termFilter("type", MediaType.SERIES.name()), new FactorBuilder().boostFactor(seriesBoost))
            .add(FilterBuilders.termFilter("type", MediaType.BROADCAST.name()), new FactorBuilder().boostFactor(broadcastBoost))
            .add(new GaussDecayFunctionBuilder("sortDate", System.currentTimeMillis(), sortDateScale).setOffset(sortDateOffset).setDecay(sortDateDecay))
        ;

        return builder.scoreMode("multiply");
    }
}
