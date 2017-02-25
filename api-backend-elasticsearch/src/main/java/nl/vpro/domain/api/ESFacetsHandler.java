/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import nl.vpro.domain.Displayable;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.ServiceLocator;

import static nl.vpro.domain.api.ESFacetsBuilder.esField;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public abstract class ESFacetsHandler {

    protected static final String ROOT_FILTER = "rootFilter";

    protected static final String FILTER_PREFIX = "filter_";

    protected static List<TermFacetResultItem> getFacetResultItems(String facetName, Facets facets) {
        if(facets != null) {
            TermsFacet facet = facets.facet(TermsFacet.class, facetName);
            if(facet == null) {
                return null;
            }
            return defaultTextFacetResult(facet);
        }
        return null;
    }

    protected static List<TermFacetResultItem> getFacetResultItems(String facetName, Facets facets, ExtendedTextFacet<?> extendedTextFacet) {
        return getFacetResultItems(esField(facetName, extendedTextFacet), facets);

    }

    protected static <T extends Enum<T>> List<TermFacetResultItem> getFacetResultItemsForEnum(String facetName, TextFacet<?> requestFacet, Facets facets, Class<T> enumClass, Function<String, T> valueOf, Function<T, String> xmlId) {
        if(facets != null) {
            TermsFacet facet = facets.facet(TermsFacet.class, facetName);
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

    protected static <T extends Enum<T>> List<TermFacetResultItem> getFacetResultItemsForEnum(String facetName, TextFacet<?> requestedFacet, Facets facets, final Class<T> enumClass) {
        return getFacetResultItemsForEnum(facetName, requestedFacet, facets, enumClass, s -> Enum.valueOf(enumClass, s), Enum::name);
    }

    protected static List<TermFacetResultItem> getBroadcasterResultItems(String facetName, Facets facets) {
        if(facets == null) {
            return null;
        }

        final TermsFacet facet = facets.facet(TermsFacet.class, facetName);
        if(facet == null) {
            return null;
        }

        return new FacetResultItemList<TermFacetResultItem>(facet) {
            @Override
            protected TermFacetResultItem adapt(int index) {
                TermsFacet.Entry entry = facet.getEntries().get(index);
                String id = entry.getTerm().string();
                Broadcaster broadcaster = ServiceLocator.getBroadcasterService().find(id);
                return new TermFacetResultItem(broadcaster != null ? broadcaster.getDisplayName() : id, id, entry.getCount());
            }
        };
    }

    protected static Terms getFilteredTerms(String facetName, Aggregation root) {
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

        Filter filter = subAggregations.getAggregations().get(FILTER_PREFIX + facetName);
        if(filter == null) {
            return null;
        }

        return filter.getAggregations().get(facetName);
    }

    protected static Aggregation getNestedResult(String prefix, String nestedField, Filter root) {
        if(root == null) {
            return null;
        }

        String aggregationName = escapePath(prefix, nestedField);
        Nested nested = root.getAggregations().get(aggregationName);
        if(nested != null) {
            return nested;
        }

        Aggregation filter = root.getAggregations().get(FILTER_PREFIX + aggregationName);
        if (filter == null) {
            // This seems to happen if a media facet is embedded in a page search....
            filter = root.getAggregations().get(FILTER_PREFIX + nestedField);
        }
        if(filter == null || !(filter instanceof HasAggregations)) {
            return null;
        }

        return ((HasAggregations)filter).getAggregations().get(aggregationName);
    }

    protected static List<DateFacetResultItem> getDateRangeFacetResultItems(DateRangeFacets<?> dateRangeFacets, String facetName, SearchResponse response, boolean asDuration) {
        Facets facets = response.getFacets();
        Aggregations aggregations = response.getAggregations();

        if(facets == null && aggregations == null) {
            return null;
        }

        List<DateFacetResultItem> dateFacetResultItems = new ArrayList<>();

        if(facets != null) {
            String prefix = facetName + ':';
            List<RangeFacet> ranges = new ArrayList<>();
            for(org.elasticsearch.search.facet.Facet facet : facets) {
                if(facet.getName().startsWith(prefix)) {
                    ranges.add((RangeFacet)facet);
                }
            }
            if(!ranges.isEmpty()) {
                dateFacetResultItems.addAll(dateRangeFacetResult(ranges, prefix));
            }
        }

        if(aggregations != null) {
            for (Aggregation aggregation : aggregations) {
                if (aggregation.getName().startsWith(facetName)) {
                    DateRangeInterval.Interval interval = ESInterval.parse(aggregation.getName().substring(facetName.length() + 1));
                    Aggregation sub = ((org.elasticsearch.search.aggregations.bucket.filter.Filter) aggregation).getAggregations().get("sub");
                    for (DateHistogram.Bucket bucket : ((DateHistogram) sub).getBuckets()) {
                        Date bucketStart = bucket.getKeyAsDate().toDate();
                        Date bucketEnd = Date.from(interval.getBucketEnd(bucket.getKeyAsDate().toDate().toInstant()));
                        DateFacetResultItem entry = new DateFacetResultItem(
                            interval.print(bucketStart.toInstant(), asDuration),
                            bucketStart,
                            bucketEnd,
                            bucket.getDocCount());

                        dateFacetResultItems.add(entry);
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

    private static int indexOf(DateFacetResultItem item, List<nl.vpro.domain.api.RangeFacet<Date>> ranges) {
        int i = 0;
        for (nl.vpro.domain.api.RangeFacet<Date> range : ranges) {
            if (range.matches(item.getBegin(), item.getEnd())) {
                return i;
            }
            i++;
        }
        return i;
    }

    protected static abstract class FacetResultItemList<T extends FacetResultItem> extends AbstractList<T> {

        private final TermsFacet facet;

        private final List<T> backing;

        public FacetResultItemList(TermsFacet facet) {
            this.facet = facet;
            this.backing = new ArrayList<>(Collections.nCopies(facet == null ? 0 : facet.getEntries().size(), (T)null));
        }

        @Override
        public T get(int index) {
            if(facet == null) {
                throw new ArrayIndexOutOfBoundsException();
            }
            T result = backing.get(index);
            if(result == null) {
                result = adapt(index);
                backing.set(index, result);
            }

            return result;
        }

        @Override
        public T set(int index, T object) {
            return backing.set(index, object);
        }

        protected abstract T adapt(int index);

        @Override
        public int size() {
            return backing.size();
        }

    }

    protected static abstract class AggregationResultItemList<A extends MultiBucketsAggregation, B extends MultiBucketsAggregation.Bucket, T extends FacetResultItem> extends AbstractList<T> {

        private List<B> buckets = new ArrayList<>();

        private final List<T> backing;

        public AggregationResultItemList(A terms) {
            buckets.addAll((Collection<? extends B>)terms.getBuckets());
            this.backing = new ArrayList<>(Collections.nCopies(buckets.size(), (T)null));
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

    protected static List<TermFacetResultItem> defaultTextFacetResult(final TermsFacet facet) {
        return new FacetResultItemList<TermFacetResultItem>(facet) {
            @Override
            protected TermFacetResultItem adapt(int index) {
                TermsFacet.Entry entry = facet.getEntries().get(index);
                return new TermFacetResultItem(entry.getTerm().string(), entry.getTerm().string(), entry.getCount());
            }
        };
    }

    protected static <T extends Enum<T>> List<TermFacetResultItem> enumTextFacetResult(final TermsFacet facet, Function<String, T> valueOf, Function<T, String> idValue) {
        return new FacetResultItemList<TermFacetResultItem>(facet) {
            @Override
            protected TermFacetResultItem adapt(int index) {
                TermsFacet.Entry entry = facet.getEntries().get(index);
                String value = entry.getTerm().string();
                String name;

                try {
                    T enumValue = valueOf.apply(value);
                    value = idValue.apply(enumValue);
                    name = ESFacetsHandler.enumValueToString(enumValue);
                } catch (IllegalArgumentException ia) {
                    log.warn(ia.getMessage());
                    name = value;
                }
                return new TermFacetResultItem(name, value, entry.getCount());
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
                    RangeFacet.Entry entry = range.getEntries().get(0);
                    String name = range.getName();
                    Double from = entry.getFrom();
                    Double to = entry.getTo();
                    result = new DateFacetResultItem(
                        name.substring(prefix.length()),
                        entry.getFromAsString() != null ? new Date(from.longValue()) : null,
                        entry.getToAsString() != null ? new Date(to.longValue()) : null,
                        entry.getCount());
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
    }

}
