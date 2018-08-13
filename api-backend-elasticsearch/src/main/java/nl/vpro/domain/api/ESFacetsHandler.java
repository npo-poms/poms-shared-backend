/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import nl.vpro.domain.Displayable;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.ServiceLocator;

import static nl.vpro.domain.api.ESFacetsBuilder.*;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public abstract class ESFacetsHandler {


    protected static <A extends Aggregation> A getAggregation(
        @Nonnull String prefix,
        @Nonnull String name,
        @Nullable HasAggregations root) {
        if (root == null) {
            return null;
        }
        HasAggregations parent = root;
        String filterName = getFilterName(prefix, name);
        HasAggregations filter = parent.getAggregations().get(filterName);
        if (filter != null) {
            parent = filter;
        }
        String subSearchName = getSubSearchName(prefix, name);
        HasAggregations subSearch = parent.getAggregations().get(subSearchName);
        if (subSearch != null) {
            parent = subSearch;
        }
        String facetName = getAggregationName(prefix, name);
        A facet = parent.getAggregations().get(facetName);
        if(facet == null) {
            return null;
        }
        return facet;
    }


    protected static <A extends Aggregation> A getNestedAggregation(
        @Nonnull String prefix,
        @Nonnull String name,
        @Nullable HasAggregations root) {
        if(root == null) {
            return null;
        }
        HasAggregations parent = root;
        String filterName = getFilterName(prefix, name);
        HasAggregations filter = parent.getAggregations().get(filterName);
        if (filter != null) {
            parent = filter;
        }

        String nestedName = getNestedName(prefix, name);
        parent =  parent.getAggregations().get(nestedName);

        if (parent == null) {
            return null;
        }

        HasAggregations subSearch = parent.getAggregations().get(getSubSearchName(prefix, name));
        if (subSearch != null) {
            parent = subSearch;
        }
        String facetName = getAggregationName(prefix, name);
        A facet = parent.getAggregations().get(facetName);
        if(facet == null) {
            return null;
        }
        return facet;
    }




    protected static List<TermFacetResultItem> getFacetResultItems(
        @Nonnull String prefix,
        @Nonnull String fieldName,
        @Nullable HasAggregations facets) {
        if(facets != null) {

            MultiBucketsAggregation a = getAggregation(prefix, fieldName, facets);
            if (a != null) {
                return defaultTextFacetResult(a);
            }
        }
        return null;
    }


    protected static List<TermFacetResultItem> getFacetResultItems(
        @Nonnull String prefix,
        @Nonnull String fieldName,
        HasAggregations rootFilter,
        @Nullable ExtendedTextFacet<?> extendedTextFacet) {
        if (extendedTextFacet != null) {
            return getFacetResultItems(
                prefix,
                esExtendedTextField(fieldName, extendedTextFacet.isCaseSensitive()),
                rootFilter
            );
        } else {
            return null;
        }

    }

    protected static <T extends Enum<T>> List<TermFacetResultItem> getFacetResultItemsForEnum(
        @Nonnull String prefix,
        @Nonnull String fieldName,
        @Nullable TextFacet<?> requestFacet,
        @Nullable HasAggregations rootFilter,
        @Nonnull Class<T> enumClass,
        @Nonnull Function<String, T> valueOf,
        @Nonnull Function<T, String> xmlId) {
        if(requestFacet != null) {

            MultiBucketsAggregation facet = getAggregation(prefix, fieldName, rootFilter);
            if(facet == null) {
                return null;
            }
            Integer threshold = requestFacet.getThreshold();
            Set<T> notFound = new HashSet<T>(Arrays.asList(enumClass.getEnumConstants()));
            List<TermFacetResultItem> result =  new ArrayList<>(enumTextFacetResult(facet, valueOf, xmlId));
            for (TermFacetResultItem i : result) {
                notFound.remove(valueOf.apply(i.getId()));
            }
            if (threshold != null && threshold == 0) {
                for (T v : notFound) {
                    result.add(emptyItem(v, xmlId));
                }
            }
            return result;
        }
        return null;
    }

    protected static <T extends Enum<T>> List<TermFacetResultItem> getFacetResultItemsForEnum(
        @Nonnull String prefix,
        @Nonnull String fieldName,
        @Nullable TextFacet<?> requestedFacet,
        @Nonnull HasAggregations facets,
        @Nonnull final Class<T> enumClass) {
        return getFacetResultItemsForEnum(
            prefix, fieldName, requestedFacet, facets, enumClass, s -> Enum.valueOf(enumClass, s), Enum::name
        );
    }

    protected static List<TermFacetResultItem> getBroadcasterResultItems(
        @Nonnull String prefix,
        @Nullable HasAggregations facets) {
        if (facets == null) {
            return null;
        }
        Terms aggregation = getAggregation(prefix, "broadcasters.id", facets);
        if(aggregation == null) {
            return null;
        }
        return new AggregationResultItemList<Terms, Terms.Bucket, TermFacetResultItem>(aggregation) {
            @Override
            protected TermFacetResultItem adapt(Terms.Bucket bucket) {
                String id = bucket.getKeyAsString();
                Broadcaster broadcaster = ServiceLocator.getBroadcasterService().find(id);
                return new TermFacetResultItem(broadcaster != null ? broadcaster.getDisplayName() : id, id, bucket.getDocCount());
            }
        };
    }

    protected static Terms getFilteredTerms(
        @Nonnull String facetName,
        Aggregation root) {
        if(root == null) {
            return null;
        }

        if(!(root instanceof HasAggregations)) {
            return null;
        }

        HasAggregations subAggregations = (HasAggregations)root;

        Terms terms = subAggregations.getAggregations().get(facetName);
        if(terms != null) {
            return terms;
        }

        Filter filter = subAggregations.getAggregations().get(getFilterName(facetName));
        if(filter == null) {
            return null;
        }

        return filter.getAggregations().get(facetName);
    }


    protected static List<DateFacetResultItem> getDateRangeFacetResultItems(
        DateRangeFacets<?> dateRangeFacets,
        @Nonnull String facetName,
        @Nonnull SearchResponse response) {
        Aggregations facets = response.getAggregations();
        Aggregations aggregations = response.getAggregations();

        if(facets == null && aggregations == null) {
            return null;
        }
        List<DateFacetResultItem> dateFacetResultItems = new ArrayList<>();
        if(aggregations != null) {
            for (Aggregation aggregation : aggregations) {
                if (aggregation.getName().startsWith(facetName)) {
                    MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;
                    List<? extends MultiBucketsAggregation.Bucket> buckets = multiBucketsAggregation.getBuckets();

                    //RangeFacet<Instant> interval;
                    //List<RangeFacet<Instant>> ranges = dateRangeFacets.getRanges();
                    for (MultiBucketsAggregation.Bucket bucket : buckets) {
                        if (bucket instanceof Range.Bucket) {
                            Range.Bucket rangeBucket = (Range.Bucket) bucket;
                            String value = bucket.getKeyAsString();
                            String name = value;
                            if (value.startsWith("PRESET:")) {
                                DateRangePreset preset = DateRangePreset.valueOf(value.substring("PRESET:".length()));
                                value = preset.getDisplayName();
                                name = preset.name();
                            }
                            DateFacetResultItem entry = DateFacetResultItem
                                .builder()
                                .value(value)
                                .name(name)
                                .begin(Instant.ofEpochMilli(((Double) rangeBucket.getFrom()).longValue()))
                                .end(Instant.ofEpochMilli(((Double) rangeBucket.getTo()).longValue()))
                                .count(bucket.getDocCount())
                                .build();
                            dateFacetResultItems.add(entry);
                        } else if (bucket instanceof Histogram.Bucket) {
                            DateFacetResultItem entry = DateFacetResultItem
                                .builder()
                                .value(bucket.getKeyAsString()) //
                                .count(bucket.getDocCount())
                                .build();
                            dateFacetResultItems.add(entry);
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }
                }
            }
        }
        if (dateRangeFacets != null) {
            // make sure the results are ordered the same as the request.
            dateFacetResultItems.sort(Comparator.comparingInt(resultItem -> indexOf(resultItem, dateRangeFacets.getRanges())));
        }
        return dateFacetResultItems;
    }

    protected static List<DurationFacetResultItem> getDurationRangeFacetResultItems(
        DurationRangeFacets<?> durationRangeFacets, String
        facetName,
        SearchResponse response) {
        Aggregations aggregations = response.getAggregations();

        if (aggregations == null) {
            return null;
        }

        List<DurationFacetResultItem> facetResultItems = new ArrayList<>();

        for (Aggregation aggregation : aggregations) {
            if (aggregation.getName().startsWith(facetName)) {
                MultiBucketsAggregation range = (MultiBucketsAggregation) aggregation;
                for (MultiBucketsAggregation.Bucket bucket : range.getBuckets()) {
                    DurationFacetResultItem entry = DurationFacetResultItem.builder()
                        .name(bucket.getKeyAsString())
                        .count(bucket.getDocCount())
                        .build();
                    facetResultItems.add(entry);
                }
            }
        }
        if (durationRangeFacets != null) {
            // make sure the results are ordered the same as the request.
            facetResultItems.sort(Comparator.comparingInt(resultItem -> indexOf(resultItem, durationRangeFacets.getRanges())));
        }
        return facetResultItems;
    }

    private static int indexOf(DateFacetResultItem item, List<nl.vpro.domain.api.RangeFacet<Instant>> ranges) {
        int i = 0;
        for (nl.vpro.domain.api.RangeFacet<Instant> range : ranges) {
            if (range.matches(item.getBegin(), item.getEnd())) {
                return i;
            }
            i++;
        }
        return i;
    }

    private static int indexOf(DurationFacetResultItem item, List<nl.vpro.domain.api.RangeFacet<Duration>> ranges) {
        int i = 0;
        for (nl.vpro.domain.api.RangeFacet<Duration> range : ranges) {
            if (range.matches(item.getBegin(), item.getEnd())) {
                return i;
            }
            i++;
        }
        return i;
    }


    protected static abstract class AggregationResultItemList<A extends MultiBucketsAggregation, B extends MultiBucketsAggregation.Bucket, T extends FacetResultItem> extends AbstractList<T> {

        private final List<B> buckets = new ArrayList<>();

        private final List<T> backing;

        public AggregationResultItemList(A terms, Predicate<B> predicate) {
            ((Collection<? extends B>)terms.getBuckets()).stream().filter(predicate).forEach(buckets::add);
            this.backing = new ArrayList<>(Collections.nCopies(buckets.size(), (T)null));
        }
        public AggregationResultItemList(A terms) {
            this(terms, (b) -> true);
        }


        @Override
        public T get(int index) {
            T result = backing.get(index);
            if(result == null) {
                result = adapt(buckets.get(index));
                backing.set(index, result);
            }

            return result;
        }

        @Override
        public T set(int index, T object) {
            return backing.set(index, object);
        }

        protected abstract T adapt(B bucket);

        @Override
        public int size() {
            return buckets.size();
        }

    }

    protected static List<TermFacetResultItem> defaultTextFacetResult(final MultiBucketsAggregation facet) {
        return new AggregationResultItemList<MultiBucketsAggregation, Terms.Bucket, TermFacetResultItem>(facet) {
            @Override
            protected TermFacetResultItem adapt(Terms.Bucket bucket) {
                return new TermFacetResultItem(bucket.getKeyAsString(), bucket.getKeyAsString(), bucket.getDocCount());
            }
        };
    }

    protected static <T extends Enum<T>> List<TermFacetResultItem> enumTextFacetResult(final MultiBucketsAggregation facet, Function<String, T> valueOf, Function<T, String> idValue) {
        return new AggregationResultItemList<MultiBucketsAggregation, Terms.Bucket, TermFacetResultItem>(facet) {
            @Override
            protected TermFacetResultItem adapt(Terms.Bucket bucket) {
                String value = bucket.getKeyAsString();
                String name;

                try {
                    T enumValue = valueOf.apply(value);
                    value = idValue.apply(enumValue);
                    name = ESFacetsHandler.enumValueToString(enumValue);
                } catch (IllegalArgumentException ia) {
                    log.warn(ia.getMessage());
                    name = value;
                }
                return new TermFacetResultItem(name, value, bucket.getDocCount());
            }
        };
    }
    protected static <T extends Enum<T>> String enumValueToString(T enumValue) {
        if (enumValue instanceof Displayable) {
            return ((Displayable) enumValue).getDisplayName();
        } else {
            return enumValue.toString();
        }
    }
    protected static <T extends Enum<T>> TermFacetResultItem emptyItem(T enumValue, Function<T, String> xmlId) {
        String name = ESFacetsHandler.enumValueToString(enumValue);
        return new TermFacetResultItem(name, xmlId.apply(enumValue), 0);

    }

    protected static List<DateFacetResultItem> dateRangeFacetResult(final List<RangeFacet> facet, final String prefix) {
        return new AbstractList<DateFacetResultItem>() {
            private final List<DateFacetResultItem> backing = new ArrayList<>(Collections.nCopies(facet.size(), (DateFacetResultItem)null));

            @Override
            public DateFacetResultItem get(int index) {
                DateFacetResultItem result = backing.get(index);
                if(result == null) {
                    RangeFacet range = facet.get(index);
                  /*  RangeFacet.Entry entry = range.getEntries().get(0);
                    String name = range.getName();
                    Double from = entry.getFrom();
                    Double to = entry.getTo();
                    result = new DateFacetResultItem(
                        name.substring(prefix.length()),
                        entry.getFromAsString() != null ? Instant.ofEpochMilli(from.longValue()) : null,
                        entry.getToAsString() != null ? Instant.ofEpochMilli(to.longValue()) : null,
                        entry.getCount());*/
                    backing.set(index, result);
                }/**/
                return result;
            }

            @Override
            public int size() {
                return backing.size();
            }
        };
    }

    protected static List<DurationFacetResultItem> durationRangeFacetResult(final List<Terms.Bucket> facet, final String prefix) {
        return new AbstractList<DurationFacetResultItem>() {
            private final List<DurationFacetResultItem> backing = new ArrayList<>(Collections.nCopies(facet.size(), (DurationFacetResultItem) null));

            @Override
            public DurationFacetResultItem get(int index) {
                DurationFacetResultItem result = backing.get(index);
                if (result == null) {
                   /* RangeFacet range = facet.get(index);
                    RangeFacet.Entry entry = range.getEntries().get(0);
                    String name = range.getName();
                    Double from = entry.getFrom();
                    Double to = entry.getTo();
                    Long fromMillis = Math.max(0, entry.getFromAsString() != null ? from.longValue() : 0);
                    Long toMillis = entry.getToAsString() != null ? to.longValue() : Long.MAX_VALUE;

                    result = new DurationFacetResultItem(
                        name.substring(prefix.length()),
                        Duration.ofMillis(fromMillis),
                        Duration.ofMillis(toMillis),
                        entry.getCount());*/
                    backing.set(index, result);
                }
                return result;
            }

            @Override
            public int size() {
                return backing.size();
            }
        };
    }


    /**
     * When using a prefix e.g., "embeds.media." as a facet or aggregation name in an search result, dots are not
     * allowed.
     */
    protected static String escapePath(String prefix, String field) {
        return (prefix + field).replace('.', '_');
    }
/*
    static class ESInterval extends DateRangeInterval.Interval {

        private ESInterval(DateRangeInterval.Unit unit, int number) {
            super(unit, number);
        }


        public String getEsValue() {
            if(number == 1) {
                return String.valueOf(unit).toLowerCase();
            } else {
                return String.valueOf(number) + unit.getShortEs();
            }
        }
        public static ESInterval parse(String input) {
            DateRangeInterval.Interval parsed = DateRangeInterval.Interval.parse(input);
            return new ESInterval(parsed.unit, parsed.number);
        }

        public String toString() {
            return (number != 1 ? String.valueOf(number) : "") + unit;
        }
    }*/


  public static <T extends AbstractSearch> List<TermFacetResultItem> filterThreshold(
        @Nullable List<TermFacetResultItem> list,
        @Nullable LimitableFacet<T> facet) {
        if (list == null) {
            return null;
        }
        if (facet == null || facet.getThreshold() == null) {
            return list;
        }
        return list.stream().filter(item -> item.getCount() >= facet.getThreshold()).collect(Collectors.toList());

    }
}
