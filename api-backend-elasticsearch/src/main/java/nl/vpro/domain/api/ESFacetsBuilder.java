/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public abstract class ESFacetsBuilder {

    public static final String TIMEZONE = "CET";

    public static final org.elasticsearch.common.joda.time.DateTimeZone DATE_TIME_ZONE = org.elasticsearch.common.joda.time.DateTimeZone.forID(TIMEZONE);

    protected static final String FILTER_PREFIX = "filter_";

    protected static void addFacet(SearchSourceBuilder searchBuilder, FilterBuilder filterBuilder, String fieldName, TextFacet<?> facet, String fieldPrefix) {
        if(facet != null) {
            TermsFacet.ComparatorType order = ESFacets.getComparatorType(facet);

            TermsFacetBuilder termsFacet = FacetBuilders
                .termsFacet(fieldName)
                .field(fieldName)
                .size(facet.getMax())
                .nested(fieldPrefix)
                .facetFilter(filterBuilder)
                .order(order);
            String include = facet.getInclude();
            if (include != null) {
                Pattern pattern = Pattern.compile(include);
                termsFacet.regex(pattern.pattern(), pattern.flags());
            }
            String script = facet.getScript();
            if (script != null) {
                termsFacet.script(script);
            }

            searchBuilder.facet(termsFacet);
        }
    }

    protected static void addFacet(SearchSourceBuilder searchBuilder, FilterBuilder filterBuilder, String fieldName, DateRangeFacets<?> facet, String fieldPrefix) {
        if(facet != null) {
            if(facet.getRanges() != null) {
                for(nl.vpro.domain.api.RangeFacet<Instant> range : facet.getRanges()) {
                    if(range instanceof DateRangeInterval) {
                        ESInterval interval = ESInterval.parse(((DateRangeInterval)range).getInterval());
                        // TODO, zou het niet logischer zijn om de verschillende aggregatie onderdeel te laten zijn van 1 'filter-aggregatie.
                        // Dat zou echter deze code nogal moeten verbouwen, want je moet het aggregationfilter doorgeven om er subAggregations aan te kunnen toevoegen.
                        searchBuilder.aggregation(
                            AggregationBuilders
                                .filter(fieldName + '_' + interval.toString())
                                .filter(filterBuilder)
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

                                        .interval(new DateHistogram.Interval(interval.getEsValue()))
                                )
                        );
                    } else {
                        RangeFacetItem<Instant> dateRangeItem = (RangeFacetItem<Instant>)range;
                        searchBuilder.facet(FacetBuilders.rangeFacet(fieldName + ':' + dateRangeItem.getName()).field(fieldName).addRange(
                            dateRangeItem.getBegin() != null ? String.valueOf(dateRangeItem.getBegin().toEpochMilli()) : null,
                            dateRangeItem.getEnd() != null ? String.valueOf(dateRangeItem.getEnd().toEpochMilli()) : null
                        )
                            .nested(fieldPrefix)
                            .facetFilter(filterBuilder));
                    }
                }
            }
        }
    }

    protected static void addFacet(SearchSourceBuilder searchBuilder, FilterBuilder filterBuilder, String fieldName, DurationRangeFacets<?> facet, String fieldPrefix) {
        if (facet != null) {
            if (facet.getRanges() != null) {
                for (nl.vpro.domain.api.RangeFacet<Duration> range : facet.getRanges()) {
                    if (range instanceof DurationRangeInterval) {
                        ESInterval interval = ESInterval.parse(((DurationRangeInterval) range).getInterval());
                        // TODO, zou het niet logischer zijn om de verschillende aggregatie onderdeel te laten zijn van 1 'filter-aggregatie.
                        // Dat zou echter deze code nogal moeten verbouwen, want je moet het aggregationfilter doorgeven om er subAggregations aan te kunnen toevoegen.
                        searchBuilder.aggregation(
                            AggregationBuilders
                                .filter(fieldName + '_' + interval.toString())
                                .filter(filterBuilder)
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

                                        .interval(new DateHistogram.Interval(interval.getEsValue()))
                                )
                        );
                    } else {
                        RangeFacetItem<Duration> dateRangeItem = (RangeFacetItem<Duration>) range;
                        searchBuilder.facet(FacetBuilders.rangeFacet(fieldName + ':' + dateRangeItem.getName()).field(fieldName).addRange(
                            dateRangeItem.getBegin() != null ? String.valueOf(dateRangeItem.getBegin().toMillis()) : null,
                            dateRangeItem.getEnd() != null ? String.valueOf(dateRangeItem.getEnd().toMillis()) : null
                        )
                            .nested(fieldPrefix)
                            .facetFilter(filterBuilder));
                    }
                }
            }
        }
    }






    private static String VALID_FACET_CHARS = "a-zA-Z0-9_-";
    private static Pattern INVALID = Pattern.compile("[^" + VALID_FACET_CHARS + "]+");
    public static String escapeFacetName(String facetName) {
        return INVALID.matcher(facetName).replaceAll("__");
    }

    protected static NestedBuilder getNestedBuilder(String pathPrefix, String nestedField, FilterBuilder subSearch, AggregationBuilder... subAggregations) {
        NestedBuilder nestedBuilder = AggregationBuilders
            .nested(escapePath(pathPrefix, nestedField))
            .path(pathPrefix + nestedField);

        if(subSearch != null) {
            FilterAggregationBuilder filteredAggregation = AggregationBuilders
                .filter(FILTER_PREFIX)
                .filter(subSearch);

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
        FilterBuilder subSearch
    ) {
        return getFilteredTermsBuilder(pathPrefix, nestedField, facetField, facet, facetField, subSearch);
    }


    protected static AggregationBuilder getFilteredTermsBuilder(
        String pathPrefix,
        String nestedField,
        String facetField,
        TextFacet<?> facet,
        String facetName,
        FilterBuilder subSearch) {

        String fullFieldPath = pathPrefix + nestedField + '.' + facetField;

        TermsBuilder termsBuilder =
            AggregationBuilders.terms(escapeFacetName(facetName))
                .field(fullFieldPath)
                .minDocCount(facet.getThreshold() == null ? -1 : facet.getThreshold())
                .size(facet.getMax())
                .order(ESFacets.getTermsOrder(facet));

        String include = facet.getInclude();
        if (include != null) {
            Pattern pattern = Pattern.compile(include);
            termsBuilder.include(pattern.pattern(), pattern.flags());
        }

        if(subSearch != null) {
            return AggregationBuilders
                .filter(FILTER_PREFIX + escapeFacetName(facetName))
                .filter(subSearch)
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


        static final Pattern PATTERN = Pattern.compile(DateRangeInterval.DATERANGE_PATTERN);

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
