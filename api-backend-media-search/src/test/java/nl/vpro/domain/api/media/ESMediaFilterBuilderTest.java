/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

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
        QueryBuilder builder = ESMediaFilterBuilder.filter(new MediaSearch());
        assertThat(toString(builder)).isEqualTo("{\n" +
            "  \"match_all\" : { }\n" +
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
                "    \"broadcasters.id\" : \"vpro\"\n" +
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
            "    \"must\" : [ {\n" +
            "      \"term\" : {\n" +
            "        \"broadcasters.id\" : \"VpRo\"\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"bool\" : {\n" +
            "        \"should\" : [ {\n" +
            "          \"term\" : {\n" +
            "            \"descendantOf.midRef\" : \"POMS_S_aa12345\"\n" +
            "          }\n" +
            "        }, {\n" +
            "          \"exists\" : {\n" +
            "            \"field\" : \"images.urn\"\n" +
            "          }\n" +
            "        } ]\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"bool\" : {\n" +
            "        \"must_not\" : {\n" +
            "          \"term\" : {\n" +
            "            \"type\" : \"VISUALRADIO\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"exists\" : {\n" +
            "        \"field\" : \"images.urn\"\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullArguments() throws Exception {
        QueryBuilder builder = ESMediaFilterBuilder.filter(new MediaSearch());
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"match_all\" : { }\n" +
                "}"
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullProfile() throws Exception {
        QueryBuilder builder = ESMediaFilterBuilder.filter((MediaSearch)null, QueryBuilders.termQuery("name", "value"));
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"term\" : {\n" +
                "    \"name\" : \"value\"\n" +
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
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition, QueryBuilders.termQuery("name", "value"));
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"and\" : {\n" +
                "    \"filters\" : [ {\n" +
                "      \"term\" : {\n" +
                "        \"broadcasters.id\" : \"Vpro\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"term\" : {\n" +
                "        \"name\" : \"value\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void testFilterLocationsWithPlatform() throws IOException {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            hasLocation("NONE")
        ));
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition, QueryBuilders.termQuery("name", "value"));
        assertThat(toString(builder)).isEqualTo(
            "{\n" +
                "  \"and\" : {\n" +
                "    \"filters\" : [ {\n" +
                "      \"nested\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"bool\" : {\n" +
                "            \"must\" : {\n" +
                "              \"exists\" : {\n" +
                "                \"field\" : \"locations.urn\"\n" +
                "              }\n" +
                "            },\n" +
                "            \"must_not\" : {\n" +
                "              \"exists\" : {\n" +
                "                \"field\" : \"locations.platform\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"path\" : \"locations\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"term\" : {\n" +
                "        \"name\" : \"value\"\n" +
                "      }\n" +
                "    } ]\n" +
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
