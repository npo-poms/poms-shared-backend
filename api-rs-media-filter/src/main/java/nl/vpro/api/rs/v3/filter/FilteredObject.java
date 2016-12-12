/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class FilteredObject<T> {

    private final FilterHelper helper;
    private final T wrapped;

    protected FilteredObject(String property, T wrapped) {
        this.helper = FilterHelper.of(property);
        this.wrapped = wrapped;
    }

    @SuppressWarnings("unchecked")
    public static <T> FilteredObject<T> wrap(String property, T object) {
        if (object instanceof FilteredObject) {
            return (FilteredObject<T>)object;
        }
        return new FilteredObject<>(property, object);
    }

    public T value() {
        return ApiMediaFilter.get().limitOrDefault(helper.property) >  0 ? wrapped : null;
    }
}
