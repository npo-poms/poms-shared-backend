/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ESMediaScoreBuilderTest {

    @Test
    public void testScore() throws Exception {
        QueryBuilder scored = ESMediaScoreBuilder.score(QueryBuilders.matchAllQuery());

        assertThat(scored.toString()).contains(
            "    \"functions\" : [ {\n" +
                "      \"filter\" : {\n" +
                "        \"exists\" : {\n" +
                "          \"field\" : \"locations\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"boost_factor\" : 2.0\n" +
                "    }, {\n" +
                "      \"filter\" : {\n" +
                "        \"term\" : {\n" +
                "          \"type\" : \"SERIES\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"boost_factor\" : 2.5\n" +
                "    }, {\n" +
                "      \"filter\" : {\n" +
                "        \"term\" : {\n" +
                "          \"type\" : \"BROADCAST\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"boost_factor\" : 1.5\n" +
                "    }, {\n" +
                "      \"gauss\" : {\n" +
                "        \"sortDate\" : {\n"
        );
    }
}
