/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.automaton.RegExp;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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

import nl.vpro.util.TriFunction;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public abstract class ESFacetsBuilder {

    public static final String FILTER_POSTFIX = "_filter";

    public static final String SUBSEARCH_POSTFIX = "_subsearch";

    public static final String FACET_POSTFIX = "_facet";

    public static final String NESTED_POSTFIX = "_nested";

    protected static <T extends AbstractSearch> TermsAggregationBuilder addFacet(
        @Nonnull String prefix,
        @Nonnull  SearchSourceBuilder searchBuilder,
        @Nonnull  String fieldName,
        @Nullable LimitableFacet<T> facet,
        @Nonnull BiConsumer<BoolQueryBuilder, T> buildFilter
        ) {
        if(facet != null) {
            Terms.Order order = ESFacets.getComparatorType(facet);


            TermsAggregationBuilder aggregationBuilder = AggregationBuilders
                .terms(getAggregationName(prefix, fieldName))
                .field(prefix + fieldName)
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

            T facetFilter = facet.getFilter();
            if (facetFilter != null) {
                BoolQueryBuilder query = QueryBuilders.boolQuery();
                buildFilter.accept(query, facetFilter);

                FilterAggregationBuilder filterAggregationBuilder =
                    AggregationBuilders.filter(getFilterName(prefix, fieldName), query);
                filterAggregationBuilder.subAggregation(aggregationBuilder);
                searchBuilder.aggregation(filterAggregationBuilder);
            } else {
                searchBuilder.aggregation(aggregationBuilder);
            }


            return aggregationBuilder;
        } else {
            return null;
        }

    }

    protected static void addDateRangeFacet(
        @Nonnull String prefix,
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nonnull String fieldName,
        @Nullable DateRangeFacets<?> facet) {
        if(facet != null) {
            if(facet.getRanges() != null) {
                RangeAggregationBuilder aggregationBuilder = null;
                for(nl.vpro.domain.api.RangeFacet<Instant> range : facet.getRanges()) {
                    if (range instanceof RangeFacetItem) {
                        if (aggregationBuilder == null) {
                            aggregationBuilder = AggregationBuilders
                                .range(prefix + fieldName)
                                .field(prefix + fieldName)
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
                            .field(prefix + fieldName)
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

    protected static DateHistogramInterval from(
        @Nonnull DateRangeInterval.Interval interval) {
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

    protected static void addDurationFacet(
        @Nonnull String prefix,
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
                                .range(prefix + fieldName)
                                .field(prefix + fieldName)
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
                            .histogram(prefix + fieldName + "." + durationRange.getInterval())
                            .interval(durationRange.getInterval().getDuration().toMillis())
                            .field(prefix + fieldName)
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

    protected static <F extends AbstractSearch, S extends AbstractSearch> void  addNestedAggregation(
        @Nonnull String prefix,
        @Nonnull  String nestedObject,
        @Nonnull  String facetField,
        @Nonnull  FilterAggregationBuilder rootAggregation,
        @Nullable SearchableLimitableFacet<F, S> facet,
        @Nonnull Function<F, QueryBuilder> filterCreator,
        @Nonnull TriFunction<S, String, String, QueryBuilder> subSearchCreator) {
        if (facet == null) {
            return;
        }

        AggregationBuilder parent = rootAggregation;


        if (facet.hasFilter()) {
            QueryBuilder query = filterCreator.apply(facet.getFilter());
            FilterAggregationBuilder filter = AggregationBuilders.filter(getFilterName(prefix, nestedObject + "/" +  facetField), query);
            rootAggregation.subAggregation(filter);
            parent = filter;
        }


        NestedAggregationBuilder nestedBuilder = AggregationBuilders
            .nested(getNestedName(prefix, nestedObject, facetField), prefix + nestedObject);


        parent.subAggregation(nestedBuilder);
        parent = nestedBuilder;


        // If the facet has a subsearch we need to wrap this aggregation in another one, which a filter.
        if (facet.hasSubSearch()) {
            QueryBuilder query = subSearchCreator.apply(facet.getSubSearch(), nestedObject, facetField);
            FilterAggregationBuilder subsearch = AggregationBuilders.filter(getSubSearchName(prefix, nestedObject + "/"  + facetField), query);
            parent.subAggregation(subsearch);
            parent = subsearch;
        }


        // Creates the actual aggregation
        TermsAggregationBuilder terms = getFilteredTermsBuilder(
            prefix,
            nestedObject,
            facetField,
            facet);
        parent.subAggregation(terms);

    }




    protected static NestedAggregationBuilder getNestedBuilder(
        @Nonnull String pathPrefix,
        @Nonnull String nestedField,
        @Nonnull String facetField,
        @Nullable QueryBuilder subSearch,
        @Nonnull AggregationBuilder... subAggregations) {


        NestedAggregationBuilder nestedBuilder = AggregationBuilders
            .nested(getNestedName(pathPrefix, nestedField, facetField), pathPrefix + nestedField)
            ;

        if(subSearch != null) {
            FilterAggregationBuilder filteredAggregation = AggregationBuilders
                .filter(getSubSearchName(pathPrefix, getNestedFieldName(nestedField, facetField)), subSearch);

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

        String aggregationName = getAggregationName(pathPrefix, nestedField, facetField);

        TermsAggregationBuilder termsBuilder =
            AggregationBuilders.terms(aggregationName)
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


    private static final String VALID_FACET_CHARS = "a-zA-Z0-9_-";
    private static final Pattern INVALID = Pattern.compile("[^" + VALID_FACET_CHARS + "]+");
    public static String escape(@Nonnull String facetName) {
        return INVALID.matcher(facetName).replaceAll("/");
    }

    public static String escape(@Nullable String prefix, @Nonnull String name) {
        if (StringUtils.isEmpty(prefix)) {
            return escape(name);
        } else {
            return escape(prefix + name);
        }
    }

    public static String getAggregationName(
        @Nonnull String fieldName) {
        return getAggregationName("", fieldName);
    }

     public static String getAggregationName(
         String prefix,
         @Nonnull String fieldName) {
        return escape(prefix, fieldName) + FACET_POSTFIX;
    }


    public static String getAggregationName(
         String prefix,
         @Nonnull String nestedObject,
         @Nonnull String fieldName) {
        return escape(prefix, getNestedFieldName(nestedObject, fieldName)) + FACET_POSTFIX;
    }
    public static String getFilterName(
        @Nonnull String fieldName) {
        return getFilterName("", fieldName);
    }


    public static String getFilterName(
        @Nonnull String prefix,
        @Nonnull String fieldName) {
        return escape(prefix, fieldName) + FILTER_POSTFIX;
    }

       public static String getFilterName(
        @Nonnull String prefix,
        @Nonnull String nestedObject,
        @Nonnull String fieldName) {
        return escape(prefix, getNestedFieldName(nestedObject, fieldName)) + FILTER_POSTFIX;
    }

    public static String getSubSearchName(
        @Nonnull String fieldName) {
        return getSubSearchName("", fieldName);
    }


    public static String getSubSearchName(
        String prefix,
        @Nonnull String fieldName) {
        return escape(prefix, fieldName) + SUBSEARCH_POSTFIX;
    }

    public static String getNestedName(
        @Nullable String prefix,
        @Nonnull String nestedField,
        @Nonnull String fieldName) {
        return escape(prefix, getNestedFieldName(nestedField, fieldName)) + NESTED_POSTFIX;
    }
    public static String getNestedFieldName(
        @Nonnull String nestField,
        @Nonnull String fieldName) {
        return nestField + "." + fieldName;
    }


    protected static String esExtendedTextField(@Nonnull String field, boolean caseSensitive) {
        return caseSensitive ? field + ".full" : field + ".lower";
    }

    protected static String esExtendedTextField(
        @Nonnull String field,
        @Nullable ExtendedTextFacet<?> facet) {
        return esExtendedTextField(field, facet == null || facet.isCaseSensitive());
    }


}
