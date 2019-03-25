/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author rico
 */

@Slf4j
public class FilteredList<T> extends AbstractList<T>  {

    private final List<T> wrapped;
    private final FilterHelper filterHelper;

    private FilteredList(String property, List<T> wrapped) {
        this.wrapped = wrapped;
        this.filterHelper = FilterHelper.of(property);
    }

    public static <T> FilteredList<T> wrap(String property, List<T> wrapped) {
        log.debug("Wrapping {}", wrapped);

        if(wrapped instanceof FilteredList) {
            FilteredList wrappedList = (FilteredList) wrapped;
            if(!(wrappedList.filterHelper.property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties " + property + " != "+ wrappedList.filterHelper.property);
            }

            return (FilteredList<T>)wrapped;
        }

        return new FilteredList<>(property, wrapped);
    }


    @Override
    public T get(int index) {
        if (filterHelper.orDefault().fromBack()) {
            return wrapped.get(wrapped.size() - size() + index);
        } else {
            return wrapped.get(index);

        }
    }

    @Override
    public int size() {
        return filterHelper.limitOr(wrapped.size());
    }

    @Override
    public T set(int index, T set) {
        filterHelper.assumeUnfiltered();
        return wrapped.set(index, set);
    }


    @Override
    public void add(int index, T add) {
        filterHelper.assumeUnfiltered();
        wrapped.add(index, add);
    }

    @Override
    public T remove(int index) {
        filterHelper.assumeUnfiltered();
        return wrapped.remove(index);
    }

}
