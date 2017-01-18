/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class FilterHelper {

    protected final String property;

    private FilterHelper(String property) {
        if (property == null) {
            throw new IllegalArgumentException("Must provide not null property value");
        }

        this.property = property.toLowerCase();
    }

    public static FilterHelper of(String property) {
        return new FilterHelper(property);
    }

    public FilterProperties orDefault() {
        return ApiMediaFilter.get().limitOrDefault(property);
    }


    public int limitOr(int size) {
        return Math.min(size, orDefault().get());
    }
    public boolean isFiltered() {
        return orDefault().get() < Integer.MAX_VALUE;
    }

    public String[] options() {
        return orDefault().getOptions();
    }

    public void assumeUnfiltered() {
        if (isFiltered()) {
            throw new UnsupportedOperationException();
        }
    }


}
