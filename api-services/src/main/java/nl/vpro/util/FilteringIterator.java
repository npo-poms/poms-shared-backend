package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.UnmodifiableIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class FilteringIterator<T> extends UnmodifiableIterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(FilteringIterator.class);


    private final Iterator<T> wrapped;
    private final Predicate<T> filter;

    private T next;

    public FilteringIterator(Iterator<T> wrapped, Predicate<T> filter) {
        this.wrapped = wrapped;
        this.filter = filter;
    }
    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public T next() {
        findNext();
        if (next == null) throw new NoSuchElementException();
        T result = next;
        next = null;
        return result;
    }

    private void findNext() {
        if (next == null) {
            while (wrapped.hasNext()) {
                next = wrapped.next();
                if (inFilter(next)) {
                    break;
                } else {
                    LOG.debug("Skipping {} while not in {}", next, filter);
                }
            }
        }

    }

    private boolean inFilter(T object) {
        return filter == null || filter.apply(object);
    }
}
