/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.api.Match;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
class ESMediaQueryBuilderTest {

    @Test
    void queryTextWithoutAForm() {
        QueryBuilder builder = ESMediaQueryBuilder.query("", new MediaSearch());

        assertThat(builder.toString()).isEqualTo(
            """
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""");
    }

    @Test
    void queryTextWithoutProfile() {
        MediaForm form = MediaFormBuilder.form().text("Text to search for").build();

        QueryBuilder builder = ESMediaQueryBuilder.query("", form.getSearches());

        assertThatJson(builder.toString()).isSimilarToResource("/query-text-without-profile.json");

    }

    @Test
    void queryForExcludeMediaIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds(Match.NOT, "POMS_12345", "POMS_12346").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            """
                {
                  "bool" : {
                    "must_not" : [
                      {
                        "terms" : {
                          "mid" : [
                            "POMS_12345",
                            "POMS_12346"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""");
    }

    @Test
    void queryForBroadcasters() {
        MediaForm form = MediaFormBuilder.form().broadcasters("VPRO", "BNN").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            """
                {
                  "bool" : {
                    "should" : [
                      {
                        "term" : {
                          "broadcasters.id" : {
                            "value" : "VPRO",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "term" : {
                          "broadcasters.id" : {
                            "value" : "BNN",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""");
    }

    @Test
    void queryForLocationsOnVaryingCase() {
        MediaForm form = MediaFormBuilder.form().locations("mP3").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            """
                {
                  "bool" : {
                    "should" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "term" : {
                                "locations.programUrl" : {
                                  "value" : "mP3",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "term" : {
                                "locations.programUrl.extension" : {
                                  "value" : "mp3",
                                  "boost" : 1.0
                                }
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""");
    }

    @Test
    void queryForTags() {
        MediaForm form = MediaFormBuilder.form().tags("Kunst", "Kunst & Cultuur").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("""
            {
              "bool" : {
                "should" : [
                  {
                    "term" : {
                      "tags.full" : {
                        "value" : "Kunst",
                        "boost" : 1.0
                      }
                    }
                  },
                  {
                    "term" : {
                      "tags.full" : {
                        "value" : "Kunst & Cultuur",
                        "boost" : 1.0
                      }
                    }
                  }
                ],
                "adjust_pure_negative" : true,
                "boost" : 1.0
              }
            }""");
    }

    @Test
    void queryForIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds("MID1", "MID2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("""
            {
              "bool" : {
                "should" : [
                  {
                    "terms" : {
                      "mid" : [
                        "MID1",
                        "MID2"
                      ],
                      "boost" : 1.0
                    }
                  }
                ],
                "adjust_pure_negative" : true,
                "boost" : 1.0
              }
            }""");
    }

    @Test
    void queryForUrnsDoesNotUseMidOptimization() {
        MediaForm form = MediaFormBuilder.form().mediaIds("urn:vpro:media:1").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).contains(
            "\"mid\"",
            "\"urn\"",
            "\"crids\""
        );
    }

    @Test
    void queryForCridsDoesNotUseMidOptimization() {
        MediaForm form = MediaFormBuilder.form().mediaIds("crid://vpro.nl/media/1").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).contains(
            "\"mid\"",
            "\"urn\"",
            "\"crids\""
        );
    }

    @Test
    void queryForGenres() {
        MediaForm form = MediaFormBuilder.form().genres("3.0.1.1", "3.0.1.2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("""
            {
              "nested" : {
                "query" : {
                  "bool" : {
                    "should" : [
                      {
                        "term" : {
                          "genres.id" : {
                            "value" : "3.0.1.1",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "term" : {
                          "genres.id" : {
                            "value" : "3.0.1.2",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                },
                "path" : "genres",
                "ignore_unmapped" : false,
                "score_mode" : "avg",
                "boost" : 1.0
              }
            }""");
    }

    @Test
    void withEverything() {
        MediaForm form = MediaForm.builder().withEverything().build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        System.out.print(builder.toString());

    }
}
