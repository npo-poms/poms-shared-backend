/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ApiMediaFilter {

    private final List<String> properties = new ArrayList<>();

    private static ThreadLocal<ApiMediaFilter> localFilter = new ThreadLocal<ApiMediaFilter>() {
        @Override
        protected ApiMediaFilter initialValue() {
            return new ApiMediaFilter();
        }
    };

    public static ApiMediaFilter get() {
        return localFilter.get();
    }

    public static void removeFilter() {
        localFilter.remove();
    }

    public void clear() {
        properties.clear();
    }

    public void filter(String... properties) {
        clear();
        add(properties);
    }

    public void add(String... properties) {
        for(String property : properties) {
            this.properties.add(property.toLowerCase());
        }
    }

    public boolean all(String property) {
        return properties.isEmpty() || properties.contains(property);
    }

    public boolean one(String property) {
        return properties.contains(property.substring(0, property.length() - 1));
    }

    public boolean none(String property) {
        return !all(property) && !one(property);
    }
}
