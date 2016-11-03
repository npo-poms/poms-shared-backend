/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ApiMediaFilter {

    private final Map<String, Integer> properties = new HashMap<>();

    // List of properties that contain a different name in JSON/XML than the fieldname in the code
    private static final Map<String, String> aliasToProperty = new HashMap<>();

    // List of properties in the code that are singularly named but are collections so need a plural property name
    private static final Map<String, String> singularToPlural = new HashMap<>();

    // List of properties that don't have a regular singular name (ie stripping the 's' or 'of' doesn't result in the correct singular form).
    private static final Map<String, String> singularExceptions = new HashMap<>();

    private boolean filtering = false;

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

    private static final ThreadLocal<ApiMediaFilter> localFilter = new ThreadLocal<ApiMediaFilter>() {
        @Override
        protected ApiMediaFilter initialValue() {
            return new ApiMediaFilter();
        }
    };

    static ApiMediaFilter get() {
        return localFilter.get();
    }

    public static void removeFilter() {
        localFilter.remove();
        assert ! localFilter.get().filtering;
    }


    public static void set(String properties) {
        if(StringUtils.isNotBlank(properties)) {
            get().filter(properties.split(","));
        } else {
            get().clear();
        }
    }

    public void clear() {
        properties.clear();
        filtering = true;
    }

    public void filter(String... properties) {
        clear();
        add(properties);
    }

    /**
     * Get singular name of a potential plural property name.
     * @param name
     * @return Singular name
     */
    private String getSingular(String name) {
        String singular;
        if (name.endsWith("s")) {
            singular = name.substring(0, name.length() - 1);
        } else if (name.endsWith("of") || name.endsWith("Of")) {
            singular = name.substring(0, name.length() - 2);
        } else {
            singular = name;
        }
        if (singularExceptions.containsKey(name)) {
            singular = singularExceptions.get(name);
        }

        return singular;

    }

    /**
     * Add a property to the filtering API.
     * @param properties One or more properties with the following syntax:
     *                   - singular name (e.g. title), defaults to 1 maximum for Lists and Sets
     *                   - plural name (e.g. segments), default to unlimited maximum for Lists and Sets
     *                   - singular or plural name with ":" + count suffix (e.g. segments:3),
     *                   will limit the amount of items returned of this List or Set to the given number if larger
     *                   than the original number of items in the wrapped List or Set.
     */
    public void add(String... properties) {
        for(String property : properties) {
            property = property.toLowerCase().trim();
            String name = aliasToProperty.getOrDefault(property, property);
            Integer max = null;

            int colon = name.indexOf(':');
            if (colon >= 1) {
                try {
                    max = Integer.parseInt(name.substring(colon+1));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid max value after ':' in " + property);
                }
                name = name.substring(0, colon);
            }

            String singular = getSingular(name);
            /* If given property name is singular, use maximum allowed = 1, otherwise maximum allowed unlimited */
            this.properties.put(singular, max != null ? max : name.equals(singular) ? 1 : Integer.MAX_VALUE);
        }
    }

    public Integer limitOrDefault(String property) {
        if (! filtering) {
            return Integer.MAX_VALUE;
        }
        String singular = getSingular(property);
        if ("title".equals(singular) || "broadcaster".equals(singular)) {
            /* title and broadcaster cannot be filtered */
            return Integer.MAX_VALUE;
        } else if (properties.isEmpty()) {
            if ("scheduleevent".equals(singular)) {
                /* scheduleEvent max 100 instead of defaultValue */
                return properties.getOrDefault(singular, 100);
            } else {
                /* If there are no properties set, show 'em all */
                return properties.getOrDefault(singular, Integer.MAX_VALUE);
            }
        } else {
            /* If there are properties, but property is not part of it, don't show it */
            return properties.getOrDefault(singular,  0);
        }
    }
}
