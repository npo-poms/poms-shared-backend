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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import nl.vpro.domain.api.ESFacetsHandler;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.support.OwnerType;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaSortHandler extends ESFacetsHandler {


    interface SortHandler extends BiFunction<MediaSortOrder, MediaObject, FieldSortBuilder> {
        @Override
        default FieldSortBuilder apply(
            @NonNull MediaSortOrder order,
            @Nullable MediaObject mediaObject) {
            return new FieldSortBuilder(order.getField().name())
                .order(order(order.getOrder()));
        }
    }

    @AllArgsConstructor
    static class DefaultSortHandler implements SortHandler {
    }

    @Getter
    @AllArgsConstructor
    static class NestedSortHandler implements SortHandler {
        private final String field;

        private final String nestedField;

        private final String filteredField;


        @Override
        public FieldSortBuilder apply(
            @NonNull MediaSortOrder sortOrder,
            @Nullable MediaObject mediaObject) {
            FieldSortBuilder sortBuilder = new FieldSortBuilder(field)
                .setNestedPath(nestedField)
                .order(order(sortOrder.getOrder()));
            if (mediaObject != null) {
                sortBuilder.setNestedFilter(QueryBuilders.termQuery(nestedField + "." + filteredField, mediaObject.getMid()));
            }
            return sortBuilder;
        }
    }

    @AllArgsConstructor
    static class TitleSortHandler implements SortHandler {

        static final String titlesField = "expandedTitles";
        static final String field = titlesField  + ".value.full";

        @Override
        public FieldSortBuilder apply(
            @NonNull MediaSortOrder sortOrder,
            @Nullable MediaObject mediaObject) {
            FieldSortBuilder sortBuilder = new FieldSortBuilder(field)
                .setNestedPath(titlesField)
                .order(order(sortOrder.getOrder()));
            if (sortOrder instanceof TitleSortOrder) {
                TitleSortOrder titleSortOrder = (TitleSortOrder) sortOrder;
                if (titleSortOrder.getType() != null || titleSortOrder.getOwner() != null) {
                    QueryBuilder nested = null;
                    if (titleSortOrder.getType() != null) {
                        nested = QueryBuilders.termQuery(titlesField + ".type", titleSortOrder.getType().name());
                    }
                    if (titleSortOrder.getOwner() != null) {
                        if (! OwnerType.ENTRIES.contains(titleSortOrder.getOwner())) {
                            throw new IllegalArgumentException("Cannot sort on type " + titleSortOrder.getOwner() + ". Can only sort on titles of type " + OwnerType.ENTRIES);
                        }
                        org.elasticsearch.index.query.TermQueryBuilder ownerFilter =
                            QueryBuilders.termQuery(titlesField + ".owner", titleSortOrder.getOwner().name());
                        if (nested == null) {
                            nested = ownerFilter;
                        } else {
                            BoolQueryBuilder bool = QueryBuilders.boolQuery();
                            bool.must(nested);
                            bool.must(ownerFilter);
                            nested = bool;
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

    public static boolean  sort(SearchSourceBuilder searchBuilder, MediaForm form, MediaObject mediaObject) {
        return sort(form, mediaObject, searchBuilder::sort);
    }

    public static boolean sort(MediaForm form, MediaObject mediaObject, Consumer<SortBuilder<?>> consumer) {
        boolean sort = false;
        if (form != null && form.isSorted()) {
            for (MediaSortOrder entry : form.getSortFields()) {
                SortHandler field = FIELDS.get(entry.getField());
                if (field == null) {
                    throw new UnsupportedOperationException("Sorting by " + entry.getField() + " is currently not supported. This may be filed as a bug!");
                }
                FieldSortBuilder sortBuilder = field.apply(entry, mediaObject);
                consumer.accept(sortBuilder);
                sort = true;
            }
        }

        // add last resort
        if (form != null && form.hasSearches()) {
            // score
            consumer.accept(SortBuilders.scoreSort());
        }
        // And even more last, if everything else equals, or undecidable, sort on descending sort date (newest stuff first)
        consumer.accept(new FieldSortBuilder(MediaSortField.sortDate.name()).order(SortOrder.DESC));
        return sort;

    }


    private static SortOrder order(Order order) {
        return order == null || order == Order.ASC ? SortOrder.ASC : SortOrder.DESC;
    }

}
