/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rico
 */
public class FilteredList<T> extends AbstractFiltered<List<T>> implements List<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FilteredSortedSet.class);

    protected FilteredList(String property, List<T> wrapped) {
        super(property, wrapped);
    }

    public static <T> FilteredList<T> wrap(String property, Object wrapped) {
        if(!(wrapped instanceof List)) {
            throw new IllegalArgumentException("Can only wrap a SortedSet");
        }

        LOG.debug("Wrapping {}", wrapped);

        if(wrapped instanceof FilteredList) {
            if(!(((FilteredList)wrapped).property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredList<T>)wrapped;
        }

        return new FilteredList<>(property, (List<T>)wrapped);
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
        if(filter.one(property)) {
            return wrapped.size() > 0 && (wrapped.get(0).equals(o));
        } else if(filter.none(property)) {
            return false;
        }
        return wrapped.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean first = true;

            final Iterator<T> wrappedIterator = wrapped.iterator();

            @Override
            public boolean hasNext() {
                if(filter.none(property)) {
                    return false;
                } else if(first) {
                    return wrappedIterator.hasNext();
                }
                return wrappedIterator.hasNext() && filter.all(property);
            }

            @Override
            public T next() {
                if(hasNext()) {
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
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        wrapped.clear();
    }

    @Override
    public T get(int index) {
        if(filter.all(property)) {
            return wrapped.get(index);
        } else if(filter.one(property)) {
            if(index == 0) {
                return wrapped.get(0);
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public T set(int index, T element) {
        if(filter.all(property)) {
            return wrapped.set(index, element);
        } else if(filter.one(property)) {
            if(index == 0) {
                return wrapped.set(0, element);
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void add(int index, T element) {
        wrapped.add(index, element);
    }

    @Override
    public T remove(int index) {
        return wrapped.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return wrapped.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return wrapped.indexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new FilteredListIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new FilteredListIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return wrapped.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return String.valueOf(wrapped);
    }

    private class FilteredListIterator implements ListIterator<T> {
        private int startIndex = 0;

        private int currentIndex = 0;

        private int endIndex = 0;

        private int lastIndexUsed = -1;

        FilteredListIterator() {
            this(0);
        }

        FilteredListIterator(int start) {
            startIndex = start;
            currentIndex = start;
            if(filter.all(property)) {
                endIndex = wrapped.size();
            } else if(filter.one(property)) {
                endIndex = 1;
            } else if(filter.none(property)) {
                endIndex = -1;
            } else {
                endIndex = wrapped.size();
            }
        }

        @Override
        public boolean hasNext() {
            return currentIndex < endIndex;
        }

        @Override
        public T next() {
            if(hasNext()) {
                lastIndexUsed = currentIndex;
                return wrapped.get(currentIndex++);
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return currentIndex > startIndex;
        }

        @Override
        public T previous() {
            if(hasPrevious()) {
                currentIndex--;
                lastIndexUsed = currentIndex;
                return wrapped.get(currentIndex);
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return this.currentIndex - this.startIndex;
        }

        @Override
        public int previousIndex() {
            return this.currentIndex - this.startIndex - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            if(lastIndexUsed >= 0) {
                wrapped.set(lastIndexUsed, t);
            } else {
                throw new IllegalStateException("must call next() or previous() before a call to set()");
            }
        }

        @Override
        public void add(T t) {
            wrapped.add(t);
        }
    }
}
