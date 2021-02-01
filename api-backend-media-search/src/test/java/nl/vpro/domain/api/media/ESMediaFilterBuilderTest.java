/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.*;
import org.junit.jupiter.api.Test;

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
    public void testFilterProfileOnNullArgument() {
        QueryBuilder builder = ESMediaFilterBuilder.filter(null);
        assertThat(toString(builder)).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"filter\" : [\n" +
            "      {\n" +
            "        \"term\" : {\n" +
            "          \"workflow\" : {\n" +
            "            \"value\" : \"PUBLISHED\",\n" +
            "            \"boost\" : 1.0\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"adjust_pure_negative\" : true,\n" +
            "    \"boost\" : 1.0\n" +
            "  }\n" +
            "}"
        );
    }

    @Test
    public void testFilterProfileOnWrappedSingleArgument() {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            broadcaster("vpro")
        ));
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"workflow\" : {\n" +
                "            \"value\" : \"PUBLISHED\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"broadcasters.id\" : {\n" +
                "            \"value\" : \"vpro\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileOnNestedArguments() {
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
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"workflow\" : {\n" +
                "            \"value\" : \"PUBLISHED\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"broadcasters.id\" : {\n" +
                "                  \"value\" : \"VpRo\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"bool\" : {\n" +
                "                \"should\" : [\n" +
                "                  {\n" +
                "                    \"term\" : {\n" +
                "                      \"descendantOf.midRef\" : {\n" +
                "                        \"value\" : \"POMS_S_aa12345\",\n" +
                "                        \"boost\" : 1.0\n" +
                "                      }\n" +
                "                    }\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"exists\" : {\n" +
                "                      \"field\" : \"images.urn\",\n" +
                "                      \"boost\" : 1.0\n" +
                "                    }\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"adjust_pure_negative\" : true,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"bool\" : {\n" +
                "                \"must_not\" : [\n" +
                "                  {\n" +
                "                    \"term\" : {\n" +
                "                      \"type\" : {\n" +
                "                        \"value\" : \"VISUALRADIO\",\n" +
                "                        \"boost\" : 1.0\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"adjust_pure_negative\" : true,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"exists\" : {\n" +
                "                \"field\" : \"images.urn\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullArguments() {
        QueryBuilder builder = ESMediaFilterBuilder.filter(null);
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"workflow\" : {\n" +
                "            \"value\" : \"PUBLISHED\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullProfile() {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("name", "value"));
        ESMediaQueryBuilder.buildMediaQuery("", boolQueryBuilder,null);
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
    public void testFilterProfileWithExtraFilter() {
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
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"workflow\" : {\n" +
                "            \"value\" : \"PUBLISHED\",\n" +
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
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterLocationsWithPlatform() {
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
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"workflow\" : {\n" +
                "            \"value\" : \"PUBLISHED\",\n" +
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
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}"
        );
    }

    private String toString(QueryBuilder builder) {
        return builder.toString();
        // We did this in ES5:
        /*
        XContentBuilder xContentBuilder = XContentFactory.jsonBuilder();
        xContentBuilder.prettyPrint();
        builder.toXContent(xContentBuilder, QueryBuilder.EMPTY_PARAMS);
        return xContentBuilder.toString();
         */
    }
}
