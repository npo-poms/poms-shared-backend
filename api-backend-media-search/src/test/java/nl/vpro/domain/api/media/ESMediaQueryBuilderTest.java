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
public class ESMediaQueryBuilderTest {

    @Test
    public void testQueryTextWithoutAForm() {
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
    public void testQueryTextWithoutProfile() {
        MediaForm form = MediaFormBuilder.form().text("Text to search for").build();

        QueryBuilder builder = ESMediaQueryBuilder.query("", form.getSearches());

        assertThatJson(builder.toString()).isSimilarToResource("/query-text-without-profile.json");

    }

    @Test
    public void testQueryForExcludeMediaIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds(Match.NOT, "POMS_12345", "POMS_12346").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());

        assertThat(builder.toString()).isEqualTo(
            """
                {
                  "bool" : {
                    "must_not" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "term" : {
                                "mid" : {
                                  "value" : "POMS_12345",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "term" : {
                                "urn" : {
                                  "value" : "POMS_12345",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "term" : {
                                "crids" : {
                                  "value" : "POMS_12345",
                                  "boost" : 1.0
                                }
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      {
                        "bool" : {
                          "should" : [
                            {
                              "term" : {
                                "mid" : {
                                  "value" : "POMS_12346",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "term" : {
                                "urn" : {
                                  "value" : "POMS_12346",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "term" : {
                                "crids" : {
                                  "value" : "POMS_12346",
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
    public void testQueryForBroadcasters() {
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
    public void testQueryForLocationsOnVaryingCase() {
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
    public void testQueryForTags() {
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
    public void testQueryForIds() {
        MediaForm form = MediaFormBuilder.form().mediaIds("MID1", "MID2").build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        assertThat(builder.toString()).isEqualTo("""
            {
              "bool" : {
                "should" : [
                  {
                    "bool" : {
                      "should" : [
                        {
                          "term" : {
                            "mid" : {
                              "value" : "MID1",
                              "boost" : 1.0
                            }
                          }
                        },
                        {
                          "term" : {
                            "urn" : {
                              "value" : "MID1",
                              "boost" : 1.0
                            }
                          }
                        },
                        {
                          "term" : {
                            "crids" : {
                              "value" : "MID1",
                              "boost" : 1.0
                            }
                          }
                        }
                      ],
                      "adjust_pure_negative" : true,
                      "boost" : 1.0
                    }
                  },
                  {
                    "bool" : {
                      "should" : [
                        {
                          "term" : {
                            "mid" : {
                              "value" : "MID2",
                              "boost" : 1.0
                            }
                          }
                        },
                        {
                          "term" : {
                            "urn" : {
                              "value" : "MID2",
                              "boost" : 1.0
                            }
                          }
                        },
                        {
                          "term" : {
                            "crids" : {
                              "value" : "MID2",
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
    public void testQueryForGenres() {
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
    public void withEverything() {
        MediaForm form = MediaForm.builder().withEverything().build();

        QueryBuilder builder = ESMediaQueryBuilder.query(form.getSearches());
        System.out.print(builder.toString());

    }
}
