/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.support.Title;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * @author rico
 * @date 24/02/2014
 * @since 3.0
 */
public class FilteredSortedTitleSet extends FilteredSortedSet<Title> {
    protected FilteredSortedTitleSet(String property, SortedSet<Title> wrapped) {
        super(property, wrapped);
    }

    public static FilteredSortedTitleSet wrap(String property, Object wrapped) {
        if(!(wrapped instanceof SortedSet)) {
            throw new IllegalArgumentException("Can only wrap a SortedSet");
        }


        if(wrapped instanceof FilteredSortedTitleSet) {
            if(!(((FilteredSortedSet)wrapped).property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredSortedTitleSet)wrapped;
        }

        return new FilteredSortedTitleSet(property, (SortedSet<Title>)wrapped);
    }

    @Override
    public Iterator<Title> iterator() {
        if(filter.one(property)) {
            return new Iterator<Title>() {
                Title next = null;

                boolean done = false;

                Title previous = null;

                final Iterator<Title> wrappedIterator = wrapped.iterator();

                @Override
                public boolean hasNext() {
                    findNext();
                    return next != null;
                }

                @Override
                public Title next() {
                    findNext();
                    if(next != null) {
                        previous = next;
                        next = null;
                        return previous;
                    }
                    throw new NoSuchElementException();
                }

                private void findNext() {
                    if(done) {
                        return;
                    }
                    if(next == null) {
                        if(wrappedIterator.hasNext()) {
                            if(previous == null) {
                                next = wrappedIterator.next();
                            } else {
                                if(previous.getType() == TextualType.SUB || previous.getType() == TextualType.EPISODE) {
                                    done = true;
                                    return;
                                }
                                while(wrappedIterator.hasNext()) {
                                    Title title = wrappedIterator.next();
                                    if(title.getType() == TextualType.SUB || title.getType() == TextualType.EPISODE) {
                                        next = title;
                                        done = true;
                                        return;
                                    }
                                }
                                done = true;
                            }
                        } else {
                            done = true;
                        }
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return super.iterator();
        }
    }
}
