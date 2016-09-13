/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.function.Predicate;

import nl.vpro.domain.media.ScheduleEvent;

/**
 * @author rico
 * @date 28/02/2014
 * @since 3.0
 */
public class ScheduleEventViewSortedSet extends FilteredSortedSet<ScheduleEvent> {
    final Predicate<ScheduleEvent> predicate = new ScheduleEventViewPredicate();

    protected ScheduleEventViewSortedSet(String property, SortedSet<ScheduleEvent> wrapped) {
        super(property, wrapped);
    }

    @Override
    public SortedSet<ScheduleEvent> headSet(ScheduleEvent toElement) {
        return wrapped.headSet(toElement);
    }

    @Override
    public SortedSet<ScheduleEvent> tailSet(ScheduleEvent fromElement) {
        return wrapped.tailSet(fromElement);
    }

    @Override
    public boolean add(ScheduleEvent scheduleEvent) {
        return predicate.test(scheduleEvent) && wrapped.add(scheduleEvent);
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof ScheduleEvent && predicate.test((ScheduleEvent) o) && wrapped.contains(o);
    }

    @Override
    public int size() {
        int count = 0;
        Iterator<ScheduleEvent> iterator = wrapped.iterator();
        while (iterator.hasNext()) {
            ScheduleEvent event = iterator.next();
            if (predicate.test(event)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return wrapped.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends ScheduleEvent> c) {
        boolean modified = false;
        for (ScheduleEvent e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return wrapped.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return wrapped.removeAll(c);
    }
}
