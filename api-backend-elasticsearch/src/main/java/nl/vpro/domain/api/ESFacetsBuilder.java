/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.apache.lucene.util.automaton.RegExp;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public abstract class ESFacetsBuilder {

    public static final String TIMEZONE = "CET";

    //public static final org.elasticsearch.common.joda.time.DateTimeZone DATE_TIME_ZONE = org.elasticsearch.common.joda.time.DateTimeZone.forID(TIMEZONE);

    protected static final String ROOT_FILTER = "rootFilter";

    protected static final String FILTER_PREFIX = "filter_";

    protected static void addFacet(
        SearchSourceBuilder searchBuilder,
        QueryBuilder filterBuilder,
        String fieldName,
        TextFacet<?> facet,
        String fieldPrefix) {
        if(facet != null) {
            Terms.Order order = ESFacets.getComparatorType(facet);

            TermsAggregationBuilder aggregationBuilder = AggregationBuilders
                .terms(fieldName)
                .field(fieldName)
                .order(order)
                .size(facet.getMax())

                ;


            String include = facet.getInclude();
            if (include != null) {
                Pattern pattern = Pattern.compile(include);
                aggregationBuilder.includeExclude(new IncludeExclude(new RegExp(pattern.pattern(), pattern.flags()), null));
            }
            String script = facet.getScript();
            if (script != null) {
                aggregationBuilder.script(new Script(script));
            }

            searchBuilder.aggregation(aggregationBuilder);
        }
    }

    protected static void addFacet(SearchSourceBuilder searchBuilder, QueryBuilder filterBuilder, String fieldName, DateRangeFacets<?> facet, String fieldPrefix) {
        if(facet != null) {
            if(facet.getRanges() != null) {
                RangeAggregationBuilder aggregationBuilder = AggregationBuilders
                    .range(fieldName)
                    .field(fieldName)
                    .keyed(true)
                    ;
                for(nl.vpro.domain.api.RangeFacet<Instant> range : facet.getRanges()) {
                    if(range instanceof DateRangeInterval) {
                        ESInterval interval = ESInterval.parse(((DateRangeInterval)range).getInterval());
                        // TODO, zou het niet logischer zijn om de verschillende aggregatie onderdeel te laten zijn van 1 'filter-aggregatie.
                        // Dat zou echter deze code nogal moeten verbouwen, want je moet het aggregationfilter doorgeven om er subAggregations aan te kunnen toevoegen.
                        searchBuilder.aggregation(
                            AggregationBuilders
                                .filter(fieldName + '_' + interval.toString(), filterBuilder)
                                .subAggregation(
                                    AggregationBuilders.dateHistogram("sub")
                                        .field(fieldName)
                                            // At least for YEARS we can make it work correctly for the CET timezone
                                            // For months I can't get it working, because in the summer CEST is used, and I can't figure
                                            // out how to get that correct. See /MGNL-11432
                                            //
                                            //.preZoneAdjustLargeInterval(true) // zou moeten werken, maar ik krijg slechts nog ES foutmeldingen dan.


                                        // Dit werkt redelijk, maar het geeft gedoe op de grenzen: Zie https://github.com/npo-poms/api/blob/master/bash/tests/pages/bucketsearches.sh
                                        // en NPA-183
                                        //.postZone((asDuration || interval.unit != IntervalUnit.YEAR) ? "00:00" : "-01:00")

                                        //.interval(AggregationBuilders.dateHistogram(interval.getEsValue()))
                                )
                        );
                    } else {
                        // TODO
                        RangeFacetItem<Instant> dateRangeItem = (RangeFacetItem<Instant>)range;
                        aggregationBuilder.addRange(
                            dateRangeItem.getBegin() != null ? dateRangeItem.getBegin().toEpochMilli() : null,
                            dateRangeItem.getEnd() != null ? dateRangeItem.getEnd().toEpochMilli() : null
                        )
                            //.sub(fieldPrefix)
                        //    .facetFilter(filterBuilder))
                        ;
                    }
                }
                searchBuilder.aggregation(aggregationBuilder);
            }
        }
    }

    protected static void addFacet(SearchSourceBuilder searchBuilder, QueryBuilder filterBuilder, String fieldName, DurationRangeFacets<?> facet, String fieldPrefix) {
        if (facet != null) {
            if (facet.getRanges() != null) {
                RangeAggregationBuilder aggregationBuilder = null;
                for (nl.vpro.domain.api.RangeFacet<Duration> range : facet.getRanges()) {
                    if (range instanceof  DurationRangeFacetItem) {
                        if (aggregationBuilder == null){
                            aggregationBuilder = AggregationBuilders
                                .range(fieldName)
                                .field(fieldName)
                                .keyed(true);
                        }
                        DurationRangeFacetItem durationRange = (DurationRangeFacetItem) range;
                        aggregationBuilder.addRange(
                            durationRange.getName(),
                            durationRange.getBegin() != null ? durationRange.getBegin().toMillis() : 0,
                            durationRange.getEnd() != null ? durationRange.getEnd().toMillis() : Double.MAX_VALUE
                        );
                    } else if (range instanceof DurationRangeInterval) {
                        DurationRangeInterval durationRange = (DurationRangeInterval) range;
                        HistogramAggregationBuilder histogramAggregationBuilder = AggregationBuilders
                            .histogram(fieldName + "." + durationRange.getInterval())
                            .interval(durationRange.parsed().getDuration().toMillis())
                            .field(fieldName)
                            .keyed(true);
                        searchBuilder.aggregation(histogramAggregationBuilder);
                    }
                }
                if (aggregationBuilder != null) {
                    searchBuilder.aggregation(aggregationBuilder);
                }
            }
        }
    }


    private static String VALID_FACET_CHARS = "a-zA-Z0-9_-";
    private static Pattern INVALID = Pattern.compile("[^" + VALID_FACET_CHARS + "]+");
    public static String escapeFacetName(String facetName) {
        return INVALID.matcher(facetName).replaceAll("__");
    }

    protected static NestedAggregationBuilder getNestedBuilder(String pathPrefix, String nestedField, QueryBuilder subSearch, AggregationBuilder... subAggregations) {
        NestedAggregationBuilder nestedBuilder = AggregationBuilders
            .nested(escapePath(pathPrefix, nestedField), pathPrefix + nestedField)
            ;

        if(subSearch != null) {
            FilterAggregationBuilder filteredAggregation = AggregationBuilders
                .filter(FILTER_PREFIX, subSearch);

            addSubAggregations(filteredAggregation, subAggregations);

            nestedBuilder.subAggregation(filteredAggregation);
        } else {
            addSubAggregations(nestedBuilder, subAggregations);
        }
        return nestedBuilder;
    }

    private static void addSubAggregations(AggregationBuilder rootAggregation, AggregationBuilder... subAggregations) {
        for(AggregationBuilder subAggregation : subAggregations) {
            rootAggregation.subAggregation(subAggregation);
        }
    }

    protected static AggregationBuilder getFilteredTermsBuilder(
        String pathPrefix,
        String nestedField,
        String facetField,
        TextFacet<?> facet,
        QueryBuilder subSearch
    ) {
        return getFilteredTermsBuilder(pathPrefix, nestedField, facetField, facet, facetField, subSearch);
    }


    protected static AggregationBuilder getFilteredTermsBuilder(
        String pathPrefix,
        String nestedField,
        String facetField,
        TextFacet<?> facet,
        String facetName,
        QueryBuilder subSearch) {

        String fullFieldPath = pathPrefix + nestedField + '.' + facetField;

        TermsAggregationBuilder termsBuilder =
            AggregationBuilders.terms(escapeFacetName(facetName))
                .field(fullFieldPath)
                .size(facet.getMax())
                .order(ESFacets.getTermsOrder(facet));
        if (facet.getThreshold() != null) {
            termsBuilder.minDocCount(facet.getThreshold());
        }

        String include = facet.getInclude();
        if (include != null) {
            Pattern pattern = Pattern.compile(include);
            termsBuilder.includeExclude(new IncludeExclude(new RegExp(pattern.pattern(), pattern.flags()), null));
        }

        if(subSearch != null) {
            return AggregationBuilders
                .filter(FILTER_PREFIX + escapeFacetName(facetName), subSearch)
                .subAggregation(termsBuilder);
        }

        return termsBuilder;
    }

    /**
     * When using a prefix e.g., "embeds.media." as a facet or aggregation name in an search result, dots are not
     * allowed.
     */
    protected static String escapePath(String prefix, String field) {
        return (prefix + field).replace('.', '_');
    }

    protected static String esField(String field, ExtendedTextFacet<?> facet) {
        return ESMatchType.esField(field, facet == null || facet.isCaseSensitive());
    }

    static class ESInterval {
        final IntervalUnit unit;

        final int number;

        private ESInterval(IntervalUnit unit, int number) {
            this.unit = unit;
            this.number = number;
        }


        static final Pattern PATTERN = Pattern.compile(DateRangeInterval.TEMPORAL_AMOUNT_INTERVAL);

        static ESInterval parse(String input) {
            java.util.regex.Matcher matcher = PATTERN.matcher(input.toUpperCase());


            if(!matcher.matches()) {
                throw new IllegalArgumentException(input);
            }
            final int number = matcher.group(1) == null ? 1 : Integer.valueOf(matcher.group(1));
            final IntervalUnit unit = IntervalUnit.valueOf(matcher.group(2));
            return new ESInterval(unit, number);
        }


        public String getEsValue() {
            if(number == 1) {
                return String.valueOf(unit).toLowerCase();
            } else {
                return String.valueOf(number) + unit.getShortEs();
            }
        }

        public String toString() {
            return (number != 1 ? String.valueOf(number) : "") + unit;
        }
    }

    enum IntervalUnit {

        YEAR,
        MONTH,
        WEEK {
            @Override
            public String getShortEs() {
                return "w";
            }
        },
        DAY {
            @Override
            public String getShortEs() {
                return "d";
            }
        },
        HOUR {
            @Override
            public String getShortEs() {
                return "h";
            }
        },
        MINUTE {
            @Override
            public String getShortEs() {
                return "m";
            }
        };

        public String getShortEs() {
            throw new UnsupportedOperationException("No multiples available for  " + this);
        }

    }
}
