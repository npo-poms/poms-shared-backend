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
        assertThat(toString(builder)).isEqualTo("""
            {
              "bool" : {
                "filter" : [
                  {
                    "term" : {
                      "workflow" : {
                        "value" : "PUBLISHED",
                        "boost" : 1.0
                      }
                    }
                  }
                ],
                "adjust_pure_negative" : true,
                "boost" : 1.0
              }
            }"""
        );
    }

    @Test
    public void testFilterProfileOnWrappedSingleArgument() {
        ProfileDefinition<MediaObject> definition = new ProfileDefinition<>(new Filter(
            broadcaster("vpro")
        ));
        QueryBuilder builder = ESMediaFilterBuilder.filter(definition);
        assertThat(toString(builder)).isEqualTo(
            """
                {
                  "bool" : {
                    "filter" : [
                      {
                        "term" : {
                          "workflow" : {
                            "value" : "PUBLISHED",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "term" : {
                          "broadcasters.id" : {
                            "value" : "vpro",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }"""
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
            """
                {
                  "bool" : {
                    "filter" : [
                      {
                        "term" : {
                          "workflow" : {
                            "value" : "PUBLISHED",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "bool" : {
                          "must" : [
                            {
                              "term" : {
                                "broadcasters.id" : {
                                  "value" : "VpRo",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "bool" : {
                                "should" : [
                                  {
                                    "term" : {
                                      "descendantOf.midRef" : {
                                        "value" : "POMS_S_aa12345",
                                        "boost" : 1.0
                                      }
                                    }
                                  },
                                  {
                                    "exists" : {
                                      "field" : "images.urn",
                                      "boost" : 1.0
                                    }
                                  }
                                ],
                                "adjust_pure_negative" : true,
                                "boost" : 1.0
                              }
                            },
                            {
                              "bool" : {
                                "must_not" : [
                                  {
                                    "term" : {
                                      "type" : {
                                        "value" : "VISUALRADIO",
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
                              "exists" : {
                                "field" : "images.urn",
                                "boost" : 1.0
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
                }"""
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullArguments() {
        QueryBuilder builder = ESMediaFilterBuilder.filter(null);
        assertThat(toString(builder)).isEqualTo(
            """
                {
                  "bool" : {
                    "filter" : [
                      {
                        "term" : {
                          "workflow" : {
                            "value" : "PUBLISHED",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }"""
        );
    }

    @Test
    public void testFilterProfileWithExtraFilterOnNullProfile() {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("name", "value"));
        ESMediaQueryBuilder.buildMediaQuery("", boolQueryBuilder,null);
        assertThat(toString(ESQueryBuilder.simplifyQuery(boolQueryBuilder))).isEqualTo(
            """
                {
                  "term" : {
                    "name" : {
                      "value" : "value",
                      "boost" : 1.0
                    }
                  }
                }"""
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
            """
                {
                  "bool" : {
                    "must" : [
                      {
                        "term" : {
                          "name" : {
                            "value" : "value",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "term" : {
                          "workflow" : {
                            "value" : "PUBLISHED",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "term" : {
                          "broadcasters.id" : {
                            "value" : "Vpro",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }"""
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
            """
                {
                  "bool" : {
                    "must" : [
                      {
                        "term" : {
                          "name" : {
                            "value" : "value",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "term" : {
                          "workflow" : {
                            "value" : "PUBLISHED",
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "nested" : {
                          "query" : {
                            "bool" : {
                              "must" : [
                                {
                                  "exists" : {
                                    "field" : "locations.urn",
                                    "boost" : 1.0
                                  }
                                }
                              ],
                              "must_not" : [
                                {
                                  "exists" : {
                                    "field" : "locations.platform",
                                    "boost" : 1.0
                                  }
                                }
                              ],
                              "adjust_pure_negative" : true,
                              "boost" : 1.0
                            }
                          },
                          "path" : "locations",
                          "ignore_unmapped" : false,
                          "score_mode" : "avg",
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }"""
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
