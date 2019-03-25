/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import javax.validation.constraints.NotNull;

import nl.vpro.util.ResortedSortedSet;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Slf4j
public class FilteredSortedSet<T> extends AbstractSet<T> implements SortedSet<T> {
    protected final SortedSet<T> wrapped;
    protected final FilterHelper filterHelper;

    protected FilteredSortedSet(String property, SortedSet<T> wrapped) {
        this.filterHelper= FilterHelper.of(property);
        this.wrapped = wrapped;
    }

    protected FilteredSortedSet(FilterHelper helper, SortedSet<T> wrapped) {
        this.filterHelper = helper;
        this.wrapped = wrapped;
    }

    public static <T> FilteredSortedSet<T> wrap(String property, Set<T> wrapped) {
        if(!(wrapped instanceof SortedSet)) {
            wrapped = new ResortedSortedSet<>(wrapped);
        }

        log.debug("Wrapping {}", wrapped);

        if(wrapped instanceof FilteredSortedSet) {
            if(!(((FilteredSortedSet)wrapped).filterHelper.property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredSortedSet<T>)wrapped;
        }

        return new FilteredSortedSet<>(property, (SortedSet<T>)wrapped);
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        if (filterHelper.isFiltered()) {
            return new Iterator<T>() {
                int count = 0;
                int limit = filterHelper.orDefault().get();

                final Iterator<T> wrappedIterator = wrapped.iterator();

                {
                    if (filterHelper.orDefault().fromBack()) {
                        if (limit < wrapped.size()) {
                            for (int i = wrapped.size() - limit; i > 0; i--) {
                                wrappedIterator.next();
                            }
                        }
                    }
                }

                @Override
                public boolean hasNext() {
                    return count < limit && wrappedIterator.hasNext();
                }

                @Override
                public T next() {
                    if (hasNext()) {
                        count++;
                        return wrappedIterator.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        } else {
            return wrapped.iterator();
        }
    }

    @Override
    public int size() {
        return filterHelper.limitOr(wrapped.size());

    }

    @Override
    public Comparator<? super T> comparator() {
        return wrapped.comparator();

    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return new FilteredSortedSet<>(filterHelper, wrapped.subSet(fromElement, toElement));

    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return new FilteredSortedSet<T>(filterHelper, wrapped.headSet(toElement));
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return new FilteredSortedSet<>(filterHelper, wrapped.tailSet(fromElement));
    }

    @Override
    public T first() {
        return iterator().next();

    }

    @Override
    public T last() {
        if (filterHelper.isFiltered()) {
            Iterator<T> it = iterator();
            T result = it.next();
            while (it.hasNext()) {
                result = it.next();
            }
            return result;
        } else {
            return wrapped.last();
        }

    }


    @Override
    public boolean add(T add) {
        //filterHelper.assumeUnfiltered();
        return wrapped.add(add);
    }

}
