/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.TextualType;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */

@Slf4j
public class ApiMediaFilter {

    private boolean throwOnUnknownProperties = true;



    private final Map<String, FilterProperties> properties = new HashMap<>();

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
        aliasToProperty.put("sortdate", "sortinstant");


        // Makes no sense, but for backwards compatibility
        aliasToProperty.put("descendant", "descendantOf");
        aliasToProperty.put("episode", "episodeOf");
        aliasToProperty.put("member", "memberOf");


        singularExceptions.put("countrie", "country");

        singularToPlural.put("email", "emails");
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
        localFilter.set(before);
        return result;
    }



    /**
     * Get singular name of a potential plural property name.
     * @param name The property name (possibly a plural)
     * @return Singular name
     */
    private String getSingular(String name) {
        String singular;
        if (name.endsWith("s")) {
            singular = name.substring(0, name.length() - 1);
        } else {
            singular = name;
        }
        if (singularExceptions.containsKey(name)) {
            singular = singularExceptions.get(name);
        }
        return singular;
    }


    private String getPlural(String name) {
        String plural  = singularToPlural.get(name);
        if (plural != null) {
            return plural;
        }

        if (name.endsWith("s")) {
            return name;
        } else {
            return name + "s";
        }
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

            String[] split = name.split(":", 3);
            String[] extra = new String[] {null};
            boolean fromBack = false;
            name = split[0];
            if (split.length > 1) {
                if (StringUtils.isNotBlank(split[split.length - 1])) {
                    try {
                        int m = Integer.parseInt(split[split.length - 1]);
                        if (m < 0) {
                            fromBack = true;
                        }
                        max = Math.abs(m);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid max value " + split[split.length - 1] + " in " + property);
                    }

                }
            }
            if (split.length > 2) {
                extra = split[1].toLowerCase().split("\\|");
            }

            if (name.toLowerCase().endsWith("of")) {
                // This makes no sense at all, but for backwardscompatibility
                // effect
                name = name + "s";
            }
            String singular = getSingular(name);
            if (max == null) {
                /* If given property name is singular, use maximum allowed = 1, otherwise maximum allowed unlimited */
                max = name.equals(singular) ? 1 : Integer.MAX_VALUE;
            }
            if (hasProperty(singular) || ! throwOnUnknownProperties) {

                for (String e : extra) {
                    FilterProperties newFilter = new FilterPropertiesImpl(max, e, fromBack);
                    FilterProperties existing = this.properties.get(singular);
                    if (existing != null) {
                        Combined combined;
                        if (existing instanceof Combined) {
                            combined = (Combined) existing;
                        } else {
                            combined = new Combined(existing);
                            this.properties.put(singular, combined);
                        }
                        combined.map.put(newFilter.getOption(), newFilter);
                    } else {
                        this.properties.put(singular, newFilter);
                    }
                }
            } else {
                throw new IllegalArgumentException("The property " + name + (name.equals(singular) ? "" : (" ( or " + singular + ")")) + " is not known. Known are : " + getKnownPropertiesForExposure());
            }
        }
    }

    private static final Set<String> KNOWN_PROPERTIES = MediaPropertiesFilters.getKnownProperties().stream().map(String::toLowerCase).collect(Collectors.toSet());
    private static Set<String> knownPropertiesForExposure = null;

    private boolean hasProperty(String singular) {
        if (MediaPropertiesFilters.isInstrumented()) {
            return KNOWN_PROPERTIES.contains(singular.toLowerCase()) || KNOWN_PROPERTIES.contains(getPlural(singular).toLowerCase());
        } else {
            log.warn("Not instrumented");
            return true;
        }
    }

    private static synchronized  Set<String> getKnownPropertiesForExposure() {
        if (knownPropertiesForExposure ==  null) {
            knownPropertiesForExposure = new HashSet<>();
            for (Class<?> t : new Class[]{Program.class, Segment.class, Group.class}) {
                while(t != null) {
                    XmlType annotation = t.getAnnotation(XmlType.class);
                    if (annotation != null) {
                        Arrays.stream(annotation.propOrder()).filter(s -> ! s.isEmpty()).forEach(s -> knownPropertiesForExposure.add(s));
                    }
                    for (Field f : t.getDeclaredFields()) {
                        XmlAttribute attribute = f.getAnnotation(XmlAttribute.class);
                        if (attribute != null) {
                            String name = attribute.name();
                            if (name.equals("##default")) {
                                name = f.getName();
                            }
                            knownPropertiesForExposure.add(name);
                        }
                    }
                    for (Method m : t.getDeclaredMethods()) {
                        XmlAttribute attribute = m.getAnnotation(XmlAttribute.class);
                        if (attribute != null) {
                            String name = attribute.name();
                            if (name.equals("##default")) {
                                name = m.getName();
                                if (name.startsWith("get") || name.startsWith("set")) {
                                    name = name.substring(3);
                                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                                }
                            }
                            knownPropertiesForExposure.add(name);
                        }
                    }
                    t = t.getSuperclass();
                }
            }
        }
        return knownPropertiesForExposure;
    }

    private Stream<String> map(String fieldName){
        switch(fieldName) {
            case "predictions":
                return Stream.of("predictionsForXml", "predictions");
        }
        return Stream.of(fieldName);
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
        add(Arrays
            .stream(properties.split(","))
            .filter(StringUtils::isNotBlank)
            .flatMap(this::map)
            .toArray(String[]::new));

        if (!this.properties.containsKey("title")) {
            this.properties.put("title", FilterProperties.one(TextualType.MAIN));
        }
        if (!this.properties.containsKey("broadcaster")) {
            this.properties.put("broadcaster", FilterProperties.ONE);
        }
    }

    FilterProperties limitOrDefault(String property) {
        String singular = getSingular(property);
        if (! filtering) {
            return FilterProperties.ALL;

        } else {
            if (retainAll) {
                // toch maar een beetje impliciet filteren voor scheduleevents want dat zijn er nogal veel soms!
                if ("scheduleevent".equals(singular)) {
                    return new FilterPropertiesImpl(100, null, true);
                } else {
                    return FilterProperties.ALL;
                }
            } else {
                return properties.getOrDefault(singular, FilterProperties.NONE);
            }
        }
    }
}
