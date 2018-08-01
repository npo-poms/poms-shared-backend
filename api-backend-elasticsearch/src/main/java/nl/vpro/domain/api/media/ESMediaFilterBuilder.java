/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.elasticsearch.index.query.BoolQueryBuilder;

import nl.vpro.domain.api.ESFilterBuilder;

/**
 * NOTE!: There is also a{@link ESMediaQueryBuilder} equivalent that more or less contains the same code for
 * building queries.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 * @TODO This can be largely dropped in favour of {@link ESMediaQueryBuilder}
 *
 */
public class ESMediaFilterBuilder extends ESFilterBuilder {

    /**
     *
     * @param prefix
     * @param searches
     * @param filter
     */
    @Deprecated

    public static void filter(String prefix, MediaSearch searches, BoolQueryBuilder filter) {
        ESMediaQueryBuilder.query(prefix, searches, filter);
    }






}
