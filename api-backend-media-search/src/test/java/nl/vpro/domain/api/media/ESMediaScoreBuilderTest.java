/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.LocalDateTime;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.media.Schedule;
import nl.vpro.test.util.jackson2.Jackson2TestUtil;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ESMediaScoreBuilderTest {

    @Test
    public void testScore() {
        QueryBuilder scored = ESMediaScoreBuilder.score(QueryBuilders.matchAllQuery(), LocalDateTime.of(2017, 9, 13, 15, 12).atZone(Schedule.ZONE_ID).toInstant());

        Jackson2TestUtil.assertThatJson(scored.toString()).isSimilarTo(
            """
                {
                  "function_score" : {
                    "query" : {
                      "match_all" : {
                        "boost" : 1.0
                      }
                    },
                    "functions" : [ {
                      "filter" : {
                        "exists" : {
                          "field" : "locations",
                          "boost" : 1.0
                        }
                      },
                      "weight" : 2.0
                    }, {
                      "filter" : {
                        "term" : {
                          "type" : {
                            "value" : "SERIES",
                            "boost" : 1.0
                          }
                        }
                      },
                      "weight" : 2.5
                    }, {
                      "filter" : {
                        "term" : {
                          "type" : {
                            "value" : "BROADCAST",
                            "boost" : 1.0
                          }
                        }
                      },
                      "weight" : 1.5
                    }, {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "script_score" : {
                        "script" : {
                          "source" : "params.gaussFactor * (doc['sortDate'].size() == 0 ? 1 : decayDateGauss(params.origin, params.scale, params.offset, params.decay, doc['sortDate'].value)) + params.gaussOffset",
                          "lang" : "painless",
                          "params" : {
                            "origin" : "2017-09-13T13:12:00Z",
                            "scale" : "157680000000ms",
                            "offset" : "604800000ms",
                            "decay" : 0.5,
                            "gaussOffset" : 0.5,
                            "gaussFactor" : 0.7
                          }
                        }
                      }
                    } ],
                    "score_mode" : "multiply",
                    "max_boost" : 2.0,
                    "boost" : 1.0
                  }
                }"""
        );
    }
}
