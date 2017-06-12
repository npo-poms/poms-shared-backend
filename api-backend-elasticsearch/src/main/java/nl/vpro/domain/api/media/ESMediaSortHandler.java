/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    private static final Map<MediaSortField, String> FIELDS = new HashMap<>(5);

    static {
        FIELDS.put(MediaSortField.title, "titles.value.full");
        FIELDS.put(MediaSortField.sortDate, "sortDate");
        FIELDS.put(MediaSortField.publishDate, "publishDate");
        FIELDS.put(MediaSortField.episode, "episodeOf.index");
        FIELDS.put(MediaSortField.episodeAdded, "episodeOf.added");
        FIELDS.put(MediaSortField.member, "memberOf.index");
        FIELDS.put(MediaSortField.memberAdded, "memberOf.added");
        FIELDS.put(MediaSortField.creationDate, "creationDate");
        FIELDS.put(MediaSortField.lastModified, "lastModified");
    }

    private static final Map<MediaSortField, String> NESTED_FIELDS = new HashMap<>(5);

    static {
        NESTED_FIELDS.put(MediaSortField.episode, "episodeOf");
        NESTED_FIELDS.put(MediaSortField.episodeAdded, "episodeOf");
        NESTED_FIELDS.put(MediaSortField.member, "memberOf");
        NESTED_FIELDS.put(MediaSortField.memberAdded, "memberOf");
    }

    public static void sort(SearchSourceBuilder searchBuilder, MediaForm form, MediaObject mediaObject) {
        sort(form, mediaObject, searchBuilder::sort);
    }

    public static void sort(MediaForm form, MediaObject mediaObject, Consumer<SortBuilder> consumer) {
        if (form != null && form.isSorted()) {
            for (Map.Entry<MediaSortField, Order> entry : form.getSortFields().entrySet()) {
                FieldSortBuilder sortBuilder;


                MediaSortField key = entry.getKey();
                String field = FIELDS.get(key);

                if (field == null) {
                    throw new UnsupportedOperationException("Sorting by " + key + " is currently not supported. This may be filed as a bug!");
                }

                if (NESTED_FIELDS.containsKey(key)) {
                    sortBuilder = new FieldSortBuilder(field)
                        .setNestedPath(NESTED_FIELDS.get(key))
                        .order(order(entry.getValue()));
                    if (mediaObject != null) {
                        sortBuilder.setNestedFilter(FilterBuilders.termFilter(NESTED_FIELDS.get(key) + ".midRef", mediaObject.getMid()));
                    }
                } else {
                    sortBuilder = new FieldSortBuilder(field)
                        .order(order(entry.getValue()));
//                .ignoreUnmapped(true);
                }
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
