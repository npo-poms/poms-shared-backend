/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.Duration;
import java.time.Instant;

import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

import nl.vpro.domain.api.GausianParameters;
import nl.vpro.domain.media.MediaType;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.weightFactorFunction;


/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaScoreBuilder {
    static GausianParameters<Duration, Instant> sortDate = new GausianParameters.Date("sortDate", Duration.ofDays(5 * 365), Duration.ofDays(7));

    static {
        sortDate.setDecay(0.5);
    }
    static float locationBoost = 2f;

    static float seriesBoost = 2.5f;

    static float broadcastBoost = 1.5f;

    static float maxBoost = 2.0f;

    public static QueryBuilder score(QueryBuilder query, Instant now) {
        FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(existsQuery("locations"), weightFactorFunction(locationBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQuery("type", MediaType.SERIES.name()), weightFactorFunction(seriesBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQuery("type", MediaType.BROADCAST.name()), weightFactorFunction(broadcastBoost)),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder(ScoreFunctionBuilders.scriptFunction(sortDate.asScript(now)))
        };
        FunctionScoreQueryBuilder builder = QueryBuilders.functionScoreQuery(query, functions);

        builder

            .scoreMode(FunctionScoreQuery.ScoreMode.SUM) // Add the individual functions scores (mainly their  boost factors) below
            .maxBoost(maxBoost) // restrict range from 0 to 2
            ;
        return builder.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY);
    }
}
