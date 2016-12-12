/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.*;

import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.support.Typable;

/**
 * For sets with member of the type 'Typable<TextualType>' (so {@link nl.vpro.domain.media.support.Title} and {@link nl.vpro.domain.media.support.Description}) we want the count to work _per textual type_.
 * @author rico
 * @author Michiel Meeuwissen
 * @since 3.0
 */
public  abstract class FilteredSortedTextualTypableSet<T extends Typable<TextualType>> extends FilteredSortedSet<T>   {

    protected FilteredSortedTextualTypableSet(String property, SortedSet<T> wrapped) {
        super(property, wrapped);

    }

    @Override
    public Iterator<T> iterator() {
        if (filterHelper.isFiltered()) {
            return groupBy().stream().flatMap(Set::stream).iterator();
        } else {
            return wrapped.iterator();
        }

    }

    @Override
    public int size() {
        if (filterHelper.isFiltered()) {
            return groupBy().stream().mapToInt(Set::size).sum();
        } else {
            return wrapped.size();
        }
    }

    List<SortedSet<T>> groupBy() {
        String[] options = filterHelper.options();
        if (options.length == 0) {
            options = new String[]{null};
        }
        Set<TextualType> textualTypes = new HashSet<>();
        for (String option : options) {
            if (option == null) {
                textualTypes.addAll(Arrays.asList(TextualType.values()));
            } else {

                textualTypes.add(TextualType.valueOf(option.toUpperCase()));
            }
        }

        List<SortedSet<T>> result = new ArrayList<>();
        TextualType textualType = null;
        SortedSet<T> forSub = null;
        for (T object : wrapped) {
            TextualType type = object.getType();
            if (type == TextualType.EPISODE) {
                type = TextualType.SUB;
            }
            if (! textualTypes.contains(type)) {
                continue;
            }

            if (forSub == null || textualType != type) {
                forSub = new TreeSet<T>();
                result.add(forSub);
                textualType = type;
            }
            FilterProperties properties = filterHelper.orDefault();
            if (properties.get(type.name().toLowerCase()) > forSub.size()) {
                forSub.add(object);
            }
        }
        return result;
    }



}
