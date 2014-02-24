/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ApiMediaFilter {

    private final List<String> properties = new ArrayList<>();

    // List of properties that contain a different name in JSON/XML than the fieldname in the code
    private static final Map<String, String> aliasToProperty = new HashMap<>();

    // List of properties in the code that are singularly named but are collections so need a plural property name
    private static final Map<String, String> singularToPlural = new HashMap<>();

    // List of properties that don't have a regular singular name (ie stripping the 's' or 'of' doesn't result in the correct singular form).
    private static final Map<String, String> singularExceptions = new HashMap<>();

    static {
        aliasToProperty.put("credit", "person");
        aliasToProperty.put("credits", "persons");
        aliasToProperty.put("exclusive", "portalrestriction");
        aliasToProperty.put("exclusives", "portalrestrictions");
        aliasToProperty.put("region", "georestriction");
        aliasToProperty.put("regions", "georestrictions");

        singularExceptions.put("countrie", "country");

        singularToPlural.put("email", "emails");
        singularToPlural.put("descendantofholder", "descendantof");
    }

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

    public static void set(String properties) {
        if (properties != null) {
            get().filter(properties.split(","));
        } else {
            get().clear();
        }
    }

    public void clear() {
        properties.clear();
    }

    public void filter(String... properties) {
        clear();
        add(properties);
    }

    public void add(String... properties) {
        for (String property : properties) {
            property = property.toLowerCase().trim();
            String name = aliasToProperty.get(property);
            if (name != null) {
                this.properties.add(name);
            } else {
                this.properties.add(property);
            }
        }
        if (!(this.properties.contains("title") || this.properties.contains("titles"))) {
            this.properties.add("title");
        }
        if (!(this.properties.contains("broadcaster") || this.properties.contains("broadcasters"))) {
            this.properties.add("broadcaster");
        }
    }

    public boolean all(String property) {
        if (singularToPlural.containsKey(property)) {
            property = singularToPlural.get(property);
        }
        return properties.isEmpty() || properties.contains(property);
    }

    public boolean one(String property) {
        String singular;
        if (singularToPlural.containsKey(property)) {
            property = singularToPlural.get(property);
        }
        if (property.endsWith("s")) {
            singular = property.substring(0, property.length() - 1);
        } else if (property.endsWith("of")) {
            singular = property.substring(0, property.length() - 2);
        } else {
            singular = property;
        }
        if (singularExceptions.containsKey(property)) {
            singular = singularExceptions.get(property);
        }
        return properties.contains(singular);
    }

    public boolean none(String property) {
        return !all(property) && !one(property);
    }

}
