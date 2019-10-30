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
            "{\n" +
                "  \"function_score\" : {\n" +
                "    \"query\" : {\n" +
                "      \"match_all\" : {\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"functions\" : [\n" +
                "      {\n" +
                "        \"filter\" : {\n" +
                "          \"exists\" : {\n" +
                "            \"field\" : \"locations\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        },\n" +
                "        \"weight\" : 2.0\n" +
                "      },\n" +
                "      {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"type\" : {\n" +
                "              \"value\" : \"SERIES\",\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"weight\" : 2.5\n" +
                "      },\n" +
                "      {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"type\" : {\n" +
                "              \"value\" : \"BROADCAST\",\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"weight\" : 1.5\n" +
                "      },\n" +
                "      {\n" +
                "        \"filter\" : {\n" +
                "          \"match_all\" : {\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        },\n" +
                "        \"gauss\" : {\n" +
                "          \"sortDate\" : {\n" +
                "            \"origin\" : 1505308320000,\n" +
                "            \"scale\" : \"157680000000ms\",\n" +
                "            \"offset\" : \"604800000ms\",\n" +
                "            \"decay\" : 0.5\n" +
                "          },\n" +
                "          \"multi_value_mode\" : \"MIN\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"score_mode\" : \"multiply\",\n" +
                "    \"max_boost\" : 2.0,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }
}
