/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.automaton.RegExp;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.index.query.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.*;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.meeuw.functional.TriFunction;

import nl.vpro.domain.media.Schedule;


/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public abstract class ESFacetsBuilder {
    private static final String FORMATTER_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMATTER_PATTERN);

    public static final String FILTER_POSTFIX = "_filter";

    public static final String NESTEDFILTER_POSTFIX = "_nestedfilter";

    public static final String SUBSEARCH_POSTFIX = "_subsearch";

    public static final String FACET_POSTFIX = "_facet";

    public static final String NESTED_POSTFIX = "_nested";

    protected static <T extends AbstractSearch<?>> TermsAggregationBuilder addFacet(
        @NonNull String prefix,
        @NonNull  FilterAggregationBuilder rootAggregation,
        @NonNull  String fieldName,
        @Nullable LimitableFacet<T> facet,
        @NonNull BiConsumer<BoolQueryBuilder, T> buildFilter
        ) {
        if(facet != null) {
            BucketOrder order = ESFacets.getComparatorType(facet);


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
                rootAggregation.subAggregation(filterAggregationBuilder);
            } else {
                rootAggregation.subAggregation(aggregationBuilder);
            }


            return aggregationBuilder;
        } else {
            return null;
        }

    }

    protected static void addDateRangeFacet(
        @NonNull String prefix,
        @NonNull FilterAggregationBuilder rootAggregation,
        @NonNull String fieldName,
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
                            .minDocCount(1)
                            ;
                        rootAggregation.subAggregation(histogramAggregationBuilder);
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
                if (aggregationBuilder != null) {
                    rootAggregation.subAggregation(aggregationBuilder);
                }
            }
        }
    }

    protected static DateHistogramInterval from(
        DateRangeInterval.@NonNull Interval interval) {
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
        @NonNull String prefix,
        @NonNull  FilterAggregationBuilder rootAggregation,
        @NonNull String fieldName,
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
                        rootAggregation.subAggregation(histogramAggregationBuilder);
                    }
                }
                if (aggregationBuilder != null) {
                    rootAggregation.subAggregation(aggregationBuilder);
                }
            }
        }
    }

     protected static <F extends AbstractSearch<?>, S extends AbstractSearch<?>> void  addNestedAggregation(
         @NonNull String prefix,
         @NonNull FilterAggregationBuilder rootAggregation,
         @NonNull String nestedObject,
         @NonNull String facetField,
         @Nullable SearchableLimitableFacet<F, S> facet,
         @NonNull Function<F, QueryBuilder> filterCreator,
         @NonNull Supplier<QueryBuilder> nestedFilterCreator,
         @NonNull TriFunction<S, String, String, QueryBuilder> subSearchCreator) {
        addNestedAggregation(
            prefix,
            rootAggregation, nestedObject,
            facetField,
            facet,
            filterCreator,
            nestedFilterCreator,
            subSearchCreator,
            () -> getName(facet, nestedObject, facetField)
        );
     }

    protected static <F extends AbstractSearch<?>, S extends AbstractSearch<?>> void  addNestedAggregation(
        @NonNull String prefix,
        @NonNull FilterAggregationBuilder rootAggregation,
        @NonNull String nestedObject,
        @NonNull String facetField,
        @Nullable SearchableLimitableFacet<F, S> facet,
        @NonNull Function<F, QueryBuilder> filterCreator,
        @NonNull Supplier<QueryBuilder> nestedFilterCreator,
        @NonNull TriFunction<S, String, String, QueryBuilder> subSearchCreator,
        @NonNull Supplier<String> nameSupplier) {
        if (facet == null) {
            return;
        }

        AggregationBuilder parent = rootAggregation;

        String name = nameSupplier.get();

        if (facet.hasFilter()) {
            QueryBuilder query = filterCreator.apply(facet.getFilter());
            FilterAggregationBuilder filter = AggregationBuilders.filter(getFilterName(prefix, name), query);
            rootAggregation.subAggregation(filter);
            parent = filter;
        }


        NestedAggregationBuilder nestedBuilder = AggregationBuilders
            .nested(getNestedName(prefix, name), prefix + nestedObject);


        parent.subAggregation(nestedBuilder);
        parent = nestedBuilder;

        QueryBuilder nestedFilter = nestedFilterCreator.get();
        if (nestedFilter != null) {
            FilterAggregationBuilder filter = AggregationBuilders.filter(getNestedFilterName(prefix, name), nestedFilter);
            parent.subAggregation(filter);
            parent = filter;
        }


        // If the facet has a subsearch we need to wrap this aggregation in another one, which a filter.
        if (facet.hasSubSearch()) {
            QueryBuilder query = subSearchCreator.apply(facet.getSubSearch(), nestedObject, facetField);
            FilterAggregationBuilder subsearch = AggregationBuilders.filter(getSubSearchName(prefix, name), query);
            parent.subAggregation(subsearch);
            parent = subsearch;
        }


        // Creates the actual aggregation
        TermsAggregationBuilder terms = getFilteredTermsBuilder(
            prefix,
            nestedObject,
            facetField,
            facet,
            nameSupplier);
        parent.subAggregation(terms);

    }



    protected static TermsAggregationBuilder getFilteredTermsBuilder(
        @NonNull  String pathPrefix,
        @NonNull  String nestedField,
        @NonNull  String facetField,
        @NonNull  LimitableFacet<?> facet,
        @NonNull Supplier<String> nameSupplier) {

        String fullFieldPath = pathPrefix + nestedField + '.' + facetField;

        String aggregationName = getAggregationName(pathPrefix, nameSupplier.get());

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
    public static String escape(@NonNull String facetName) {
        return INVALID.matcher(facetName).replaceAll("/");
    }

    public static String escape(@Nullable String prefix, @NonNull String name) {
        if (StringUtils.isEmpty(prefix)) {
            return escape(name);
        } else {
            return escape(prefix + name);
        }
    }

    public static String getAggregationName(
        @NonNull String name) {
        return getAggregationName("", name);
    }

     public static String getAggregationName(
         String prefix,
         @NonNull String name) {
        return escape(prefix, name) + FACET_POSTFIX;
    }


    public static String getFilterName(
        @NonNull String name) {
        return getFilterName("", name);
    }


    public static String getFilterName(
        @NonNull String prefix,
        @NonNull String name) {
        return escape(prefix, name) + FILTER_POSTFIX;
    }

    public static String getNestedFilterName(
        @NonNull String prefix,
        @NonNull String name) {
        return escape(prefix, name) + NESTEDFILTER_POSTFIX;
    }



    public static String getSubSearchName(
        @NonNull String name) {
        return getSubSearchName("", name);
    }


    public static String getSubSearchName(
        @NonNull String prefix,
        @NonNull String name) {
        return escape(prefix, name) + SUBSEARCH_POSTFIX;
    }


    public static String getNestedName(
        @Nullable String prefix,
        @NonNull String name) {
        return escape(prefix, name) + NESTED_POSTFIX;
    }

    public static String getNestedFieldName(
        @NonNull String nestField,
        @NonNull String fieldName) {
        return nestField + "." + fieldName;
    }

    public static String getName(Facet<?> facet, String nestedObject, String facetField) {
        return facet instanceof Nameable ? ((Nameable) facet).getName() : getNestedFieldName(nestedObject, facetField);
    }


    protected static String esExtendedTextField(@NonNull String field, boolean caseSensitive) {
        return caseSensitive ? field + ".full" : field + ".lower";
    }

    public static String esExtendedTextField(
        @NonNull String field,
        @Nullable ExtendedTextFacet<?> facet) {
        return esExtendedTextField(field, facet == null || facet.isCaseSensitive());
    }


}
