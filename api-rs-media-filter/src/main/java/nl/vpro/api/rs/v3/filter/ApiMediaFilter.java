/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
    private Boolean retainAll = null;


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

    private static final ThreadLocal<ApiMediaFilter> localFilter = ThreadLocal.withInitial(ApiMediaFilter::new);

    static ApiMediaFilter get() {
        return localFilter.get();
    }

    public static void removeFilter() {
        localFilter.remove();
        assert ! localFilter.get().filtering;
    }


    /**
     * <code>null</code>: No filtering
     * "": No filtering
     * "all": No filtering
     * "none": Return very limited set of properties (actually: title,broadcaster)
     * <property>[s][:<number>],...:
     */
    public static void set(String properties) {
       get().filter(properties);

    }

    public static <T> T doWithout(Callable<T> callable) {
        ApiMediaFilter before = get();
        removeFilter();
        T result;
        try {
            result = callable.call();
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        localFilter.set(before);;
        return result;
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
    private void add(String... properties) {
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
    private void filter(String properties) {
        this.properties.clear();

        if ("all".equals(properties)) {
            filtering = false;
            retainAll = null;
            return;
        }

        filtering = true;
        if ("".equals(properties) || properties == null) {
            retainAll = true;
            return;
        }

        retainAll = false;
        if ("none".equals(properties)) {
            properties = "";
        }
        add(Arrays.stream(properties.split(",")).filter(StringUtils::isNotBlank).toArray(String[]::new));
        if (!this.properties.containsKey("title")) {
            this.properties.put("title", 1);
        }
        if (!this.properties.containsKey("broadcaster")) {
            this.properties.put("broadcaster", 1);
        }
    }

    Integer limitOrDefault(String property) {
        String singular = getSingular(property);
        if (! filtering) {
            return Integer.MAX_VALUE;

        } else {
            if (retainAll) {
                // toch maar een beetje filteren voor scheduleevents want dat zijn er nogal veel soms!
                if ("scheduleevent".equals(singular)) {
                    return 100;
                } else {
                    return Integer.MAX_VALUE;
                }
            } else {
                return properties.getOrDefault(singular, 0);
            }
        }
    }
}
