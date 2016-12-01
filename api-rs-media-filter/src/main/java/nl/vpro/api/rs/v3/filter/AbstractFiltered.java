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

    protected String property;

    protected int max = Integer.MAX_VALUE;

    protected AbstractFiltered(String property, T wrapped) {
        if (property == null) {
            throw new IllegalArgumentException("Must provide not null property value");
        }
        if (wrapped == null) {
            throw new IllegalArgumentException("Must provide not null property field to wrap");
        }

        int colon = property.indexOf(':');
        if (colon >= 1) {
            this.property = property.toLowerCase().substring(0, colon);
            try {
                this.max = Integer.parseInt(property.substring(colon + 1));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Must provide an integer max after the property name " + property);
            }
        } else {
            this.property = property.toLowerCase();
        }

        this.wrapped = wrapped;
    }

    protected ApiMediaFilter getFilter() {
        return ApiMediaFilter.get();
    }

}
