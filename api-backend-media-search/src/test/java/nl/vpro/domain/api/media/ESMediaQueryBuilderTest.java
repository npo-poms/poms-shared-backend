/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

import nl.vpro.domain.api.Match;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaQueryBuilderTest {

    @Test
    public void testQueryTextWithoutAForm() {
        QueryBuilder builder = ESMediaQueryBuilder.query("", new MediaSearch());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryTextWithoutProfile() {
        MediaForm form = MediaFormBuilder.form().text("Text to search for").build();

        QueryBuilder builder = ESMediaQueryBuilder.query("", form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"broadcasters.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"broadcasters.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 4.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"countries.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"countries.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.4\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"credits.fullName.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"credits.fullName.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 4.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"descriptions.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.2\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"descriptions.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"descriptions.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"genres.terms.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"genres.terms.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 4.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"images.title\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"images.title\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.2\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"images.title.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"images.title.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 3.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"images.description\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"images.description\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.2\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"images.description.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"images.description.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"portals.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"portals.value.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 3.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"segments.tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.2\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"segments.tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"segments.tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.4\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.1\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"segments.titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.2\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"segments.titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.5\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"segments.titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 3.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.2\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"tags.text\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.4\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 2.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"tags.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 4.0\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 1.3\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"titles.value\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 2.6\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match\" : {\n" +
                "                \"titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"operator\" : \"OR\",\n" +
                "                  \"prefix_length\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"fuzzy_transpositions\" : true,\n" +
                "                  \"lenient\" : false,\n" +
                "                  \"zero_terms_query\" : \"NONE\",\n" +
                "                  \"boost\" : 3.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase\" : {\n" +
                "                \"titles.stemmed\" : {\n" +
                "                  \"query\" : \"Text to search for\",\n" +
                "                  \"slop\" : 4,\n" +
                "                  \"boost\" : 6.0\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"disable_coord\" : false,\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForExcludeMediaIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds(Match.NOT, "POMS_12345", "POMS_12346").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"must_not\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"mid\" : {\n" +
                "                  \"value\" : \"POMS_12345\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"urn\" : {\n" +
                "                  \"value\" : \"POMS_12345\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"crids\" : {\n" +
                "                  \"value\" : \"POMS_12345\",\n" +
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
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"mid\" : {\n" +
                "                  \"value\" : \"POMS_12346\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"urn\" : {\n" +
                "                  \"value\" : \"POMS_12346\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"crids\" : {\n" +
                "                  \"value\" : \"POMS_12346\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"disable_coord\" : false,\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForBroadcasters() {
        MediaForm form = MediaFormBuilder.form().broadcasters("VPRO", "BNN").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : [\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"broadcasters.id\" : {\n" +
                "            \"value\" : \"VPRO\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"term\" : {\n" +
                "          \"broadcasters.id\" : {\n" +
                "            \"value\" : \"BNN\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForLocationsOnVaryingCase() {
        MediaForm form = MediaFormBuilder.form().locations("mP3").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            "{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"locations.programUrl\" : {\n" +
                "                  \"value\" : \"mP3\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"term\" : {\n" +
                "                \"locations.programUrl.extension\" : {\n" +
                "                  \"value\" : \"mp3\",\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"disable_coord\" : false,\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"disable_coord\" : false,\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testQueryForTags() {
        MediaForm form = MediaFormBuilder.form().tags("Kunst", "Kunst & Cultuur").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"should\" : [\n" +
            "      {\n" +
            "        \"term\" : {\n" +
            "          \"tags.full\" : {\n" +
            "            \"value\" : \"Kunst\",\n" +
            "            \"boost\" : 1.0\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"term\" : {\n" +
            "          \"tags.full\" : {\n" +
            "            \"value\" : \"Kunst & Cultuur\",\n" +
            "            \"boost\" : 1.0\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"disable_coord\" : false,\n" +
            "    \"adjust_pure_negative\" : true,\n" +
            "    \"boost\" : 1.0\n" +
            "  }\n" +
            "}");
    }

    @Test
    public void testQueryForIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds("MID1", "MID2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"should\" : [\n" +
            "      {\n" +
            "        \"bool\" : {\n" +
            "          \"should\" : [\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"mid\" : {\n" +
            "                  \"value\" : \"MID1\",\n" +
            "                  \"boost\" : 1.0\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"urn\" : {\n" +
            "                  \"value\" : \"MID1\",\n" +
            "                  \"boost\" : 1.0\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"crids\" : {\n" +
            "                  \"value\" : \"MID1\",\n" +
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
            "        \"bool\" : {\n" +
            "          \"should\" : [\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"mid\" : {\n" +
            "                  \"value\" : \"MID2\",\n" +
            "                  \"boost\" : 1.0\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"urn\" : {\n" +
            "                  \"value\" : \"MID2\",\n" +
            "                  \"boost\" : 1.0\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"term\" : {\n" +
            "                \"crids\" : {\n" +
            "                  \"value\" : \"MID2\",\n" +
            "                  \"boost\" : 1.0\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"disable_coord\" : false,\n" +
            "          \"adjust_pure_negative\" : true,\n" +
            "          \"boost\" : 1.0\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"disable_coord\" : false,\n" +
            "    \"adjust_pure_negative\" : true,\n" +
            "    \"boost\" : 1.0\n" +
            "  }\n" +
            "}");
    }

    @Test
    public void testQueryForGenres() {
        MediaForm form = MediaFormBuilder.form().genres("3.0.1.1", "3.0.1.2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("{\n" +
            "  \"nested\" : {\n" +
            "    \"query\" : {\n" +
            "      \"bool\" : {\n" +
            "        \"should\" : [\n" +
            "          {\n" +
            "            \"term\" : {\n" +
            "              \"genres.id\" : {\n" +
            "                \"value\" : \"3.0.1.1\",\n" +
            "                \"boost\" : 1.0\n" +
            "              }\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"term\" : {\n" +
            "              \"genres.id\" : {\n" +
            "                \"value\" : \"3.0.1.2\",\n" +
            "                \"boost\" : 1.0\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"disable_coord\" : false,\n" +
            "        \"adjust_pure_negative\" : true,\n" +
            "        \"boost\" : 1.0\n" +
            "      }\n" +
            "    },\n" +
            "    \"path\" : \"genres\",\n" +
            "    \"ignore_unmapped\" : false,\n" +
            "    \"score_mode\" : \"avg\",\n" +
            "    \"boost\" : 1.0\n" +
            "  }\n" +
            "}");
    }

    @Test
    public void withEverything() {
        MediaForm form = MediaForm.builder().withEverything().build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        System.out.print(builder.toString());

    }
}
