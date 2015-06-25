/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.util.SortedSet;

import nl.vpro.domain.media.ScheduleEvent;

/**
 * @author rico
 * @since 3.0
 */
public class ScheduleEventView {
    public static SortedSet<ScheduleEvent> wrap(Object wrapped) {
        if(!(wrapped instanceof SortedSet)) {
            throw new IllegalArgumentException("Can only wrap a SortedSet");
        }
        return new ScheduleEventViewSortedSet((SortedSet<ScheduleEvent>)wrapped);
    }
}
