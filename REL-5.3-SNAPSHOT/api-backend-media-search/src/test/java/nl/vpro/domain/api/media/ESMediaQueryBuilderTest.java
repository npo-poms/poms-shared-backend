/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import nl.vpro.domain.api.Match;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaQueryBuilderTest {

    @Test
    public void testQueryTextWithoutAForm() throws Exception {
        QueryBuilder builder = ESMediaQueryBuilder.query(new MediaSearch());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"match_all\" : { }\n" +
                "}");
    }

    @Test
    public void testQueryTextWithoutProfile() throws Exception {
        MediaForm form = MediaFormBuilder.form().text("Text to search for").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"should\" : [ {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"broadcasters.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"broadcasters.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 4.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"countries.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"countries.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.4,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"credits.fullName.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"credits.fullName.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 4.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.2,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"genres.terms.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"genres.terms.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 4.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"images.title\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"images.title\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.2,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"images.title.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"images.title.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 3.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"images.description\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"images.description\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.2,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"images.description.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"images.description.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"portals.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"portals.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 3.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.2,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.4,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.2,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 3.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.4,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 4.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 1.3\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 2.6,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"match\" : {\n" +
                "                \"titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"boolean\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 3.0\n" +
                "                }\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"match\" : {\n" +
                "                \"titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"type\" : \"phrase\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"boost\" : 6.0,\n" +
                "                  \"slop\" : 4\n" +
                "                }\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForExcludeMediaIds() throws Exception {
        MediaForm form = MediaFormBuilder.form().mediaIds(Match.NOT, "POMS_12345", "POMS_12346").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [ {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"term\" : {\n" +
                "                \"mid\" : \"POMS_12345\"\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"term\" : {\n" +
                "                \"urn\" : \"POMS_12345\"\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"term\" : {\n" +
                "                \"crids\" : \"POMS_12345\"\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"term\" : {\n" +
                "                \"mid\" : \"POMS_12346\"\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"term\" : {\n" +
                "                \"urn\" : \"POMS_12346\"\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"term\" : {\n" +
                "                \"crids\" : \"POMS_12346\"\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForBroadcasters() throws Exception {
        MediaForm form = MediaFormBuilder.form().broadcasters("VPRO", "BNN").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"should\" : [ {\n" +
                "          \"term\" : {\n" +
                "            \"broadcasters.id\" : \"VPRO\"\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"term\" : {\n" +
                "            \"broadcasters.id\" : \"BNN\"\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForLocationsOnVaryingCase() throws Exception {
        MediaForm form = MediaFormBuilder.form().locations("mP3").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"should\" : {\n" +
                "          \"bool\" : {\n" +
                "            \"should\" : [ {\n" +
                "              \"term\" : {\n" +
                "                \"locations.programUrl\" : \"mP3\"\n" +
                "              }\n" +
                "            }, {\n" +
                "              \"term\" : {\n" +
                "                \"locations.programUrl.extension\" : \"mp3\"\n" +
                "              }\n" +
                "            } ]\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForTags() throws Exception {
        MediaForm form = MediaFormBuilder.form().tags("Kunst", "Kunst & Cultuur").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : {\n" +
            "      \"bool\" : {\n" +
            "        \"should\" : [ {\n" +
            "          \"term\" : {\n" +
            "            \"tags\" : \"Kunst\"\n" +
            "          }\n" +
            "        }, {\n" +
            "          \"term\" : {\n" +
            "            \"tags\" : \"Kunst & Cultuur\"\n" +
            "          }\n" +
            "        } ]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }

    @Test
    public void testQueryForIds() throws Exception {
        MediaForm form = MediaFormBuilder.form().mediaIds("MID1", "MID2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : {\n" +
            "      \"bool\" : {\n" +
            "        \"should\" : [ {\n" +
            "          \"bool\" : {\n" +
            "            \"should\" : [ {\n" +
            "              \"term\" : {\n" +
            "                \"mid\" : \"MID1\"\n" +
            "              }\n" +
            "            }, {\n" +
            "              \"term\" : {\n" +
            "                \"urn\" : \"MID1\"\n" +
            "              }\n" +
            "            }, {\n" +
            "              \"term\" : {\n" +
            "                \"crids\" : \"MID1\"\n" +
            "              }\n" +
            "            } ]\n" +
            "          }\n" +
            "        }, {\n" +
            "          \"bool\" : {\n" +
            "            \"should\" : [ {\n" +
            "              \"term\" : {\n" +
            "                \"mid\" : \"MID2\"\n" +
            "              }\n" +
            "            }, {\n" +
            "              \"term\" : {\n" +
            "                \"urn\" : \"MID2\"\n" +
            "              }\n" +
            "            }, {\n" +
            "              \"term\" : {\n" +
            "                \"crids\" : \"MID2\"\n" +
            "              }\n" +
            "            } ]\n" +
            "          }\n" +
            "        } ]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }

    @Test
    public void testQueryForGenres() throws Exception {
        MediaForm form = MediaFormBuilder.form().genres("3.0.1.1", "3.0.1.2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : {\n" +
            "      \"nested\" : {\n" +
            "        \"query\" : {\n" +
            "          \"bool\" : {\n" +
            "            \"should\" : [ {\n" +
            "              \"term\" : {\n" +
            "                \"genres.id\" : \"3.0.1.1\"\n" +
            "              }\n" +
            "            }, {\n" +
            "              \"term\" : {\n" +
            "                \"genres.id\" : \"3.0.1.2\"\n" +
            "              }\n" +
            "            } ]\n" +
            "          }\n" +
            "        },\n" +
            "        \"path\" : \"genres\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }
}
