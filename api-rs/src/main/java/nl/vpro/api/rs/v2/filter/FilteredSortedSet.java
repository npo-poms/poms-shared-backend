/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class FilteredSortedSet<T> extends AbstractFiltered<SortedSet<T>> implements SortedSet<T> {
    private static final Logger log = LoggerFactory.getLogger(FilteredSortedSet.class);

    protected FilteredSortedSet(String property, SortedSet<T> wrapped) {
        super(property, wrapped);
    }

    public static <T> FilteredSortedSet<T> wrap(String property, Object wrapped) {
        if(!(wrapped instanceof SortedSet)) {
            throw new IllegalArgumentException("Can only wrap a SortedSet");
        }

        log.debug("Wrapping {}", wrapped);

        if(wrapped instanceof FilteredSortedSet) {
            if(!(((FilteredSortedSet)wrapped).property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredSortedSet<T>)wrapped;
        }

        return new FilteredSortedSet<>(property, (SortedSet<T>)wrapped);
    }

    @Override
    public Comparator<? super T> comparator() {
        return wrapped.comparator();
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return wrapped.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T first() {
        if(filter.none(property)) {
            throw new NoSuchElementException();
        }
        return wrapped.first();
    }

    @Override
    public T last() {
        if(filter.all(property)) {
            return wrapped.last();
        }
        return first();
    }

    @Override
    public int size() {
        if(filter.all(property)) {
            return wrapped.size();
        } else if(filter.one(property)) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean first = true;

            final Iterator<T> wrappedIterator = wrapped.iterator();

            @Override
            public boolean hasNext() {
                if (filter.none(property)) {
                    return false;
                } else if (first) {
                    return wrappedIterator.hasNext();
                }
                return wrappedIterator.hasNext() && filter.all(property);
            }

            @Override
            public T next() {
                if (hasNext()) {
                    first = false;
                    return wrappedIterator.next();
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return wrapped.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return wrapped.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return wrapped.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return wrapped.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
         wrapped.clear();
    }
}