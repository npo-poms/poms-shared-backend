/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.Instant;

import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;

import nl.vpro.domain.media.MediaType;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.*;


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

    public static QueryBuilder score(QueryBuilder query, Instant now) {

        FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(existsQuery("locations"), fieldValueFactorFunction("locations").factor(locationBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQuery("type", MediaType.SERIES.name()), weightFactorFunction(seriesBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQuery("type", MediaType.BROADCAST.name()), weightFactorFunction(broadcastBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQuery("type", MediaType.BROADCAST.name()), weightFactorFunction(broadcastBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(gaussDecayFunction("sortDate", now.toEpochMilli(), sortDateScale))
                //.setOffset(sortDateOffset)
                //.setDecay(sortDateDecay))
        };
        FunctionScoreQueryBuilder builder = functionScoreQuery(functions);

        builder
            .scoreMode(FiltersFunctionScoreQuery.ScoreMode.SUM) // Add the individual functions scores (mainly their  boost factors) below
            .maxBoost(maxBoost) // restrict range from 0 to 2
            ;
        return builder.scoreMode(FiltersFunctionScoreQuery.ScoreMode.MULTIPLY);
    }
}
