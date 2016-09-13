/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.SortedSet;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.domain.media.Schedule;
import nl.vpro.domain.media.ScheduleEvent;

/**
 * @author rico
 * @since 3.0
 */
public class ScheduleEventViewPredicate implements Predicate<ScheduleEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleEventViewPredicate.class);

    private final Instant stop;

    public ScheduleEventViewPredicate() {
        Collection<? extends GrantedAuthority> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        int daysToAdd = 3;
        for(GrantedAuthority role : roles) {
            String roleName = role.getAuthority();
            if(roleName != null) {
                switch(roleName) {
                    case "ROLE_API_USER":
                    case "ROLE_API_SUPERUSER":
                    case "ROLE_API_SUPERCLIENT":
                    case "ROLE_API_CHANGES_SUPERCLIENT":
                        daysToAdd = -1;
                        break;
                    case "ROLE_API_CLIENT":
                    case "ROLE_API_CHANGES_CLIENT":
                        daysToAdd = 3;
                        break;
                    default:
                        break;
                }
            }
        }
        if(daysToAdd > 0) {
            Instant now = Instant.now();
            stop = now.plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MILLIS);
            LOG.debug("Setting stop to " + stop);
        } else {
            stop = null;
        }
    }

    @Override
    public boolean test(ScheduleEvent input) {
        return stop == null || (input.getStartInstant() != null && input.getStartInstant().isBefore(stop));
    }

    private static Date guideDayStart(Date guideDay) {
        ZonedDateTime dateTime = guideDay.toInstant().atZone(Schedule.ZONE_ID).toLocalDate().atTime(Schedule.START_OF_SCHEDULE).plusDays(1).atZone(Schedule.ZONE_ID);
        return Date.from(dateTime.toInstant());
    }
}
