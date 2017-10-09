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
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
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
                RangeAggregationBuilder aggregationBuilder = null;
                for(nl.vpro.domain.api.RangeFacet<Instant> range : facet.getRanges()) {
                    if (range instanceof DateRangeFacetItem) {
                        if (aggregationBuilder == null) {
                            aggregationBuilder = AggregationBuilders
                                .range(fieldName)
                                .field(fieldName)
                                .keyed(true);
                        }
                        DateRangeFacetItem dateRange = (DateRangeFacetItem) range;
                        aggregationBuilder.addRange(
                            dateRange.getName(),
                            dateRange.getBegin() != null ? dateRange.getBegin().toEpochMilli() : Instant.MIN.toEpochMilli(),
                            dateRange.getEnd() != null ? dateRange.getEnd().toEpochMilli() : Instant.MAX.toEpochMilli()
                        );
                    } else if(range instanceof DateRangeInterval) {
                        DateRangeInterval dateRange = (DateRangeInterval) range;

                        DateHistogramAggregationBuilder histogramAggregationBuilder = AggregationBuilders
                            .dateHistogram(fieldName + "." + dateRange.getInterval())
                            .dateHistogramInterval(from(dateRange.getInterval()))
                            .field(fieldName)
                            .keyed(false)
                            ;
                        searchBuilder.aggregation(histogramAggregationBuilder);
                    } else if (range instanceof DateRangePreset) {
                        DateRangePreset preset = (DateRangePreset) range;
                        if (aggregationBuilder == null) {
                            aggregationBuilder = AggregationBuilders
                                .range(fieldName)
                                .field(fieldName)
                                .keyed(true);
                        }
                        aggregationBuilder.addRange(
                            preset.getName(),
                            preset.getBegin() != null ? preset.getBegin().toEpochMilli() : Instant.MIN.toEpochMilli(),
                            preset.getEnd() != null ? preset.getEnd().toEpochMilli() : Instant.MAX.toEpochMilli()
                        );
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
                if (aggregationBuilder != null) {
                    searchBuilder.aggregation(aggregationBuilder);
                }
            }
        }
    }

    protected static DateHistogramInterval from(DateRangeInterval.Interval interval) {
        switch(interval.getUnit()) {
            case YEAR:
                return interval.amount == 1 ? DateHistogramInterval.YEAR : DateHistogramInterval.days(interval.amount * 365);
            case MONTH:
                return interval.amount == 1 ? DateHistogramInterval.MONTH : DateHistogramInterval.days(interval.amount * 30);
            case WEEK:
                return DateHistogramInterval.weeks(interval.amount);
            case DAY:
                return DateHistogramInterval.days(interval.amount);
            case HOUR:
                return DateHistogramInterval.hours(interval.amount);
            case MINUTE:
                return DateHistogramInterval.minutes(interval.amount);
            default:
                throw new IllegalArgumentException();
        }
    }

    protected static void addFacet(
        SearchSourceBuilder searchBuilder,
        QueryBuilder filterBuilder,
        String fieldName,
        DurationRangeFacets<?> facet,
        String fieldPrefix) {
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
                            .interval(durationRange.getInterval().getDuration().toMillis())
                            .field(fieldName)
                            .format("bla")
                            .keyed(false);
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


}
