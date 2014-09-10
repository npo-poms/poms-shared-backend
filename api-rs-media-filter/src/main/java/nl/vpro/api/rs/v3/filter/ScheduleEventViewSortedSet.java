/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import com.google.common.base.Predicate;
import nl.vpro.domain.media.ScheduleEvent;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * @author rico
 * @date 28/02/2014
 * @since 3.0
 */
public class ScheduleEventViewSortedSet implements SortedSet<ScheduleEvent> {
    final Predicate<ScheduleEvent> predicate = new ScheduleEventViewPredicate();

    protected final SortedSet<ScheduleEvent> wrapped;

    public ScheduleEventViewSortedSet(SortedSet<ScheduleEvent> wrapped) {
        this.wrapped = wrapped;
        for(ScheduleEvent event : wrapped) {
            if(!predicate.apply(event)) {
                wrapped.remove(event);
            }
        }
    }


    @Override
    public Iterator<ScheduleEvent> iterator() {
        return wrapped.iterator();
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public Comparator<? super ScheduleEvent> comparator() {
        return wrapped.comparator();
    }

    @Override
    public SortedSet<ScheduleEvent> subSet(ScheduleEvent fromElement, ScheduleEvent toElement) {
        return wrapped.subSet(fromElement, toElement);
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
    public ScheduleEvent first() {
        return wrapped.first();
    }

    @Override
    public ScheduleEvent last() {
        return wrapped.last();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrapped.contains(o);
    }

    @Override
    public Object[] toArray() {
        return wrapped.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return wrapped.toArray(a);
    }

    @Override
    public boolean add(ScheduleEvent scheduleEvent) {
        if(predicate.apply(scheduleEvent)) {
            return wrapped.add(scheduleEvent);
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return wrapped.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return wrapped.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends ScheduleEvent> c) {
        boolean modified = false;
        for(ScheduleEvent e : c) {
            if(add(e)) {
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

    @Override
    public void clear() {
        wrapped.clear();
    }
}
