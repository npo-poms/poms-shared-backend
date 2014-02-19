/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

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
        return null;
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return null;
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
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;

            Iterator<T> wrappedIterator = wrapped.iterator();

            private T next;

            @Override
            public boolean hasNext() {
                if(filter.none(property)) {
                    return false;
                } else if(index++ == 0) {
                    return wrappedIterator.hasNext();
                }
                return wrappedIterator.hasNext() && filter.all(property);
            }

            @Override
            public T next() {
                return wrappedIterator.next();
            }

            @Override
            public void remove() {
                iterator().remove();
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
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}