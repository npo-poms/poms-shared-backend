/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class FilteredObject<T> extends AbstractFiltered<T> {

    protected FilteredObject(String property, T wrapped) {
        super((property.indexOf(':') >= 0 ? property.substring(0, property.indexOf(':')) : property) + ":1", wrapped);
    }

    @SuppressWarnings("unchecked")
    public static <T> FilteredObject<T> wrap(String property, T object) {
        if (object instanceof FilteredObject) {
            return (FilteredObject<T>)object;
        }
        return new FilteredObject<>(property, object);
    }

    public T value() {
        return getFilter().limitOrDefault(property) >  0 ? wrapped : null;
    }
}
