/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class AbstractFiltered<T> {
    protected final T wrapped;

    protected final ApiMediaFilter filter = ApiMediaFilter.get();

    protected final String property;

    protected AbstractFiltered(String property, T wrapped) {
        if(property == null) {
            throw new IllegalArgumentException("Must provide not null property value");
        }
        if(wrapped == null) {
            throw new IllegalArgumentException("Must provide not null property field to wrap");
        }

        this.property = property.toLowerCase();
        this.wrapped = wrapped;
    }
}
