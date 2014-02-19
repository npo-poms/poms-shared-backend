/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;


import nl.vpro.domain.constraint.AbstractFilter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class FilteredObject<T> extends AbstractFiltered<T> {

    protected FilteredObject(String property, T wrapped) {
        super(property, wrapped);
    }

    public static <T> FilteredObject<T> wrap(String property, T object) {
        return new FilteredObject<>(property, object);
    }

    public T value() {
        return filter.none(property) ? null : wrapped;
    }
}
