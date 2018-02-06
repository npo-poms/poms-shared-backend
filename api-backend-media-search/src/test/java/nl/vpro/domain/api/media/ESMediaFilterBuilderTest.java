/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import nl.vpro.domain.api.ESQueryBuilder;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.media.Filter;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;

import static nl.vpro.domain.constraint.media.MediaConstraints.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaFilterBuilderTest {

    @Test
    public void testFilterProfileOnNullArgument() throws Exception {
        QueryBuilder builder = ESMediaFilterBuilder.filter(null);
        assertThat(toString(builder)).isEqualTo("{\n" +
            "  \"match_all\" : {\n" +
            "    \"boost\" : 1.0\n" +
            "  }\n" +
            "}"
        );
    }

    @Test
    public void testFilterProfileOnWrappedSingleArgument() throws Exception {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            broadcaster("vpro")
        ));
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"term\" : {\n" +
                "    \"broadcasters.id\" : {\n" +
                "      \"value\" : \"vpro\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileOnNestedArguments() throws Exception {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            and(
                broadcaster("VpRo"),
                or(
                    descendantOf("POMS_S_aa12345"),
                    hasImage()
                ),
                not(
                    mediaType(MediaType.VISUALRADIO)
                ),
                hasImage()
            )
        ));
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"broadcasters.id\" : {\n" +
                "            \"value\" : \"VpRo\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"descendantOf.midRef\" : {\n" +
                "                  \"value\" : \"POMS_S_aa12345\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"exists\" : {\n" +
                "                \"field\" : \"images.urn\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"disable_coord\" : false,\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"must_not\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"type\" : {\n" +
                "                  \"value\" : \"VISUALRADIO\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"disable_coord\" : false,\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"exists\" : {\n" +
                "          \"field\" : \"images.urn\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullArguments() throws Exception {
        QueryBuilder builder = ESMediaFilterBuilder.filter(null);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullProfile() throws Exception {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("name", "value"));
        ESMediaFilterBuilder.filter("", (MediaSearch)null, boolQueryBuilder);
        assertThat(toString(ESQueryBuilder.simplifyQuery(boolQueryBuilder))).isEqualTo(
            "{\n" +
                "  \"term\" : {\n" +
                "    \"name\" : {\n" +
                "      \"value\" : \"value\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFilterProfileWithExtraFilter() throws Exception {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            broadcaster("Vpro")
        ));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("name", "value"));

        ESMediaFilterBuilder.filter(definition, boolQueryBuilder);
        assertThat(toString(boolQueryBuilder)).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"name\" : {\n" +
                "            \"value\" : \"value\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"broadcasters.id\" : {\n" +
                "            \"value\" : \"Vpro\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterLocationsWithPlatform() throws IOException {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            hasLocation("NONE")
        ));
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        builder.must(QueryBuilders.termQuery("name", "value"));
        ESMediaFilterBuilder.filter(definition, builder);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"name\" : {\n" +
                "            \"value\" : \"value\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"nested\" : {\n" +
                "          \"query\" : {\n" +
                "            \"bool\" : {\n" +
                "              \"must\" : [\n" +
                "                {\n" +
                "                  \"exists\" : {\n" +
                "                    \"field\" : \"locations.urn\",\n" +
                "                    \"boost\" : 1.0\n" +
                "                  }\n" +
                "                }\n" +
                "              ],\n" +
                "              \"must_not\" : [\n" +
                "                {\n" +
                "                  \"exists\" : {\n" +
                "                    \"field\" : \"locations.platform\",\n" +
                "                    \"boost\" : 1.0\n" +
                "                  }\n" +
                "                }\n" +
                "              ],\n" +
                "              \"disable_coord\" : false,\n" +
                "              \"adjust_pure_negative\" : true,\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"path\" : \"locations\",\n" +
                "          \"ignore_unmapped\" : false,\n" +
                "          \"score_mode\" : \"avg\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    /**
     * In the current ES release the toString override on FilterBuilder is missing...
     */
    private String toString(QueryBuilder builder) throws IOException {
        XContentBuilder xContentBuilder = XContentFactory.jsonBuilder();
        xContentBuilder.prettyPrint();
        builder.toXContent(xContentBuilder, QueryBuilder.EMPTY_PARAMS);
        return xContentBuilder.string();
    }
}
