/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        int limit = filter.limitOrDefault(property);
        if (limit > 0) {
            return wrapped.first();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T last() {
        int limit = filter.limitOrDefault(property);
        /* todo this is weird behaviour, but was previously implemented like this */
        if (limit == Integer.MAX_VALUE) {
            return wrapped.last();
        }
        return first();
    }

    @Override
    public int size() {
        int limit = filter.limitOrDefault(property);
        int size = wrapped.size();

        return limit < size ? limit : size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        int limit = filter.limitOrDefault(property);
        boolean found = false;
        if (limit > 0) {
            for (T i : this) {
                if (o.equals(i)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int count = 0;
            int limit = filter.limitOrDefault(property);

            final Iterator<T> wrappedIterator = wrapped.iterator();

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

    @Override
    public String toString() {
        return String.valueOf(new ArrayList<>(this));
    }
}
