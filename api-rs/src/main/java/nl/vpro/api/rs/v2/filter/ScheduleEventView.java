/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import com.google.common.collect.Sets;
import nl.vpro.domain.media.ScheduleEvent;

import java.util.SortedSet;

/**
 * @author rico
 * @date 27/02/2014
 * @since 3.0
 */
public class ScheduleEventView {
    public static SortedSet<ScheduleEvent> wrap(Object wrapped) {
        if (!(wrapped instanceof SortedSet)) {
            throw new IllegalArgumentException("Can only wrap a SortedSet");
        }
        return Sets.filter((SortedSet<ScheduleEvent>) wrapped, new ScheduleEventViewPredicate());
    }
}
