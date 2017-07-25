/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import nl.vpro.domain.api.ESFacetsHandler;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaSortHandler extends ESFacetsHandler {


    interface SortHandler extends BiFunction<MediaSortOrder, MediaObject, FieldSortBuilder> {
        @Override
        default FieldSortBuilder apply(MediaSortOrder order, MediaObject mediaObject) {
            return new FieldSortBuilder(order.getSortField().name())
                .order(order(order.getOrder()));
        }
    }

    @AllArgsConstructor
    static class DefaultSortHandler implements SortHandler {
    }

    @AllArgsConstructor
    static class NestedSortHandler implements SortHandler {
        @Getter
        private final String field;

        @Getter
        private final String nestedField;

        @Getter
        private final String filteredField;


        @Override
        public FieldSortBuilder apply(MediaSortOrder sortOrder, MediaObject mediaObject) {
            FieldSortBuilder sortBuilder = new FieldSortBuilder(field)
                .setNestedPath(nestedField)
                .order(order(sortOrder.getOrder()));
            if (mediaObject != null) {
                sortBuilder.setNestedFilter(FilterBuilders.termFilter(nestedField + "." + filteredField, mediaObject.getMid()));
            }
            return sortBuilder;
        }
    }

    @AllArgsConstructor
    static class TitleSortHandler implements SortHandler {
        static String field = "titles.value.full";

        @Override
        public FieldSortBuilder apply(MediaSortOrder sortOrder, MediaObject mediaObject) {
            FieldSortBuilder sortBuilder = new FieldSortBuilder(field)
                .setNestedPath("titles")
                .order(order(sortOrder.getOrder()));
            if (sortOrder instanceof TitleSortOrder) {
                TitleSortOrder titleSortOrder = (TitleSortOrder) sortOrder;
                if (titleSortOrder.getTextualType() != null || titleSortOrder.getOwnerType() != null) {
                    FilterBuilder nested = null;
                    if (titleSortOrder.getTextualType() != null) {
                        nested = FilterBuilders.termFilter("titles.type", titleSortOrder.getTextualType());
                    }
                    if (titleSortOrder.getOwnerType() != null) {
                        FilterBuilder ownerFilter = FilterBuilders.termFilter("titles.owner", titleSortOrder.getOwnerType());
                        if (nested == null) {
                            nested = ownerFilter;
                        } else {
                            nested = FilterBuilders.andFilter(nested, ownerFilter);
                        }
                    }
                    sortBuilder.setNestedFilter(nested);
                }

            }

            return sortBuilder;
        }
    }


    private static final Map<MediaSortField, SortHandler> FIELDS = new HashMap<>(5);


    static {
        FIELDS.put(MediaSortField.title, new TitleSortHandler());
        FIELDS.put(MediaSortField.sortDate, new DefaultSortHandler());
        FIELDS.put(MediaSortField.publishDate, new DefaultSortHandler());
        FIELDS.put(MediaSortField.episode, new NestedSortHandler("episodeOf.index", "episodeOf", "midRef"));
        FIELDS.put(MediaSortField.episodeAdded, new NestedSortHandler("episodeOf.added", "episodeOf", "midRef"));
        FIELDS.put(MediaSortField.member, new NestedSortHandler("memberOf.index", "memberOf", "midRef"));
        FIELDS.put(MediaSortField.memberAdded, new NestedSortHandler("memberOf.added", "memberOf", "midRef"));
        FIELDS.put(MediaSortField.creationDate, new DefaultSortHandler());
        FIELDS.put(MediaSortField.lastModified, new DefaultSortHandler());
    }

    public static void sort(SearchSourceBuilder searchBuilder, MediaForm form, MediaObject mediaObject) {
        sort(form, mediaObject, searchBuilder::sort);
    }

    public static void sort(MediaForm form, MediaObject mediaObject, Consumer<SortBuilder> consumer) {
        if (form != null && form.isSorted()) {
            for (MediaSortOrder entry : form.getSortFields()) {
                SortHandler field = FIELDS.get(entry.getSortField());
                if (field == null) {
                    throw new UnsupportedOperationException("Sorting by " + entry.getSortField() + " is currently not supported. This may be filed as a bug!");
                }
                FieldSortBuilder sortBuilder = field.apply(entry, mediaObject);
                consumer.accept(sortBuilder);
            }
        }

        // add last resort
        consumer.accept(SortBuilders.fieldSort("_score").order(SortOrder.DESC));

    }


    private static SortOrder order(Order order) {
        return order == null || order == Order.ASC ? SortOrder.ASC : SortOrder.DESC;
    }

}
