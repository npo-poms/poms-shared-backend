/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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


    public static final String FILTER_POSTFIX = "_filter";

    public static final String SUBSEARCH_POSTFIX = "_subsearch";


    public static final String FACET_POSTFIX = "_facet";

    public static final String NESTED_POSTFIX = "_nested";


    /**
     * Abstract because type of facet parameter is unknown.
     */
    protected static TermsAggregationBuilder createAggregationBuilder(
        @Nonnull  String fieldName,
        @Nullable LimitableFacet<?> facet) {
        if(facet != null) {
            Terms.Order order = ESFacets.getComparatorType(facet);


            TermsAggregationBuilder aggregationBuilder = AggregationBuilders
                .terms(getAggregationName(fieldName))
                .field(fieldName)
                .order(order)
                .size(facet.getMax())

                ;


            String include = facet.getInclude();
            if (include != null) {
                Pattern pattern = Pattern.compile(include);
                aggregationBuilder.includeExclude(
                    new IncludeExclude(new RegExp(pattern.pattern(), pattern.flags()), null)
                );
            }
            String script = facet.getScript();
            if (script != null) {
                aggregationBuilder.script(new Script(script));
            }
            return aggregationBuilder;
        } else {
            return null;
        }

    }

    protected static void addFacet(SearchSourceBuilder searchBuilder, String fieldName, DateRangeFacets<?> facet) {
        if(facet != null) {
            if(facet.getRanges() != null) {
                RangeAggregationBuilder aggregationBuilder = null;
                for(nl.vpro.domain.api.RangeFacet<Instant> range : facet.getRanges()) {
                    if (range instanceof RangeFacetItem) {
                        if (aggregationBuilder == null) {
                            aggregationBuilder = AggregationBuilders
                                .range(fieldName)
                                .field(fieldName)
                                .keyed(false)
                            ;
                        }
                        RangeFacetItem<Instant> dateRange = (RangeFacetItem) range;
                        String name;
                        if (range instanceof DateRangePreset) {
                            name = "PRESET:" + dateRange.getName();
                        } else {
                            name = dateRange.getName();
                        }
                        aggregationBuilder.addRange(
                            name,
                            dateRange.getBegin() != null ? dateRange.getBegin().toEpochMilli() : Double.MIN_VALUE,
                            dateRange.getEnd() != null ? dateRange.getEnd().toEpochMilli() : Double.MAX_VALUE
                        );
                    } else if (range instanceof DateRangeInterval) {
                        DateRangeInterval dateRange = (DateRangeInterval) range;
                        String name = fieldName + "." + dateRange.getInterval();
                        String format = dateRange.getInterval().getUnit().getFormat();

                        DateHistogramAggregationBuilder histogramAggregationBuilder = AggregationBuilders
                            .dateHistogram(name)
                            .dateHistogramInterval(from(dateRange.getInterval()))
                            .field(fieldName)
                            .format(format)
                            .keyed(false)
                            ;
                        searchBuilder.aggregation(histogramAggregationBuilder);
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
                return interval.amount == 1 ? DateHistogramInterval.WEEK : DateHistogramInterval.days(interval.amount * 7);
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
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nonnull String fieldName,
        @Nullable DurationRangeFacets<?> facet) {
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
                            .format("interval" + durationRange.getIntervalString())
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

    protected static NestedAggregationBuilder getNestedBuilder(
        @Nonnull String pathPrefix,
        @Nonnull String nestedField,
        @Nullable QueryBuilder subSearch,
        @Nonnull AggregationBuilder... subAggregations) {


        NestedAggregationBuilder nestedBuilder = AggregationBuilders
            .nested(getNestedName(pathPrefix, nestedField), pathPrefix + nestedField)
            ;

        if(subSearch != null) {
            FilterAggregationBuilder filteredAggregation = AggregationBuilders
                .filter(getFilterName(nestedField + "_subsearch"), subSearch);

            addSubAggregations(filteredAggregation, subAggregations);

            nestedBuilder.subAggregation(filteredAggregation);
        } else {
            addSubAggregations(nestedBuilder, subAggregations);
        }
        return nestedBuilder;
    }

    private static void addSubAggregations(
        @Nonnull AggregationBuilder rootAggregation,
        @Nonnull AggregationBuilder... subAggregations) {
        for(AggregationBuilder subAggregation : subAggregations) {
            rootAggregation.subAggregation(subAggregation);
        }
    }


    protected static TermsAggregationBuilder getFilteredTermsBuilder(
        @Nonnull  String pathPrefix,
        @Nonnull  String nestedField,
        @Nonnull  String facetField,
        @Nonnull  LimitableFacet<?> facet) {

        String fullFieldPath = pathPrefix + nestedField + '.' + facetField;


        TermsAggregationBuilder termsBuilder =
            AggregationBuilders.terms(
                getAggregationName(nestedField, facetField))
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


        return termsBuilder;
    }




    public static String getAggregationName(String fieldName) {
        return escapeFacetName(fieldName) + FACET_POSTFIX;
    }

     public static String getAggregationName(String path, String fieldName) {
        return getAggregationName(path + "_" + fieldName);
    }

    public static String getFilterName(String fieldName) {
        return escapeFacetName(fieldName) + FILTER_POSTFIX;
    }


    public static String getFilterName(String path, String fieldName) {
        return getFilterName(path + "_" + fieldName);
    }

    public static String getSubSearchName(String fieldName) {
        return escapeFacetName(fieldName) + SUBSEARCH_POSTFIX;
    }


    public static String getSubSearchName(String path, String fieldName) {
        return getSubSearchName(path + "_" + fieldName);
    }

    public static String getNestedName(String path, String fieldName) {
        return escapeFacetName(path + "_" + fieldName) + NESTED_POSTFIX;
    }


    /**
     * When using a prefix e.g., "embeds.media." as a facet or aggregation name in an search result, dots are not
     * allowed.
     */
    protected static String escapePath(@Nonnull String prefix, @Nonnull String field) {
        return (prefix + field).replace('.', '_');
    }

    protected static String esField(@Nonnull String field, boolean caseSensitive) {
        return caseSensitive ? field + ".full" : field + ".lower";
    }

    protected static String esField(
        @Nonnull String field,
        @Nullable ExtendedTextFacet<?> facet) {
        return esField(field, facet == null || facet.isCaseSensitive());
    }


}
