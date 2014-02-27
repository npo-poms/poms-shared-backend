/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import com.google.common.base.Predicate;
import nl.vpro.domain.media.ScheduleEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * @author rico
 * @date 27/02/2014
 * @since version
 */
public class ScheduleEventViewPredicate implements Predicate<ScheduleEvent> {
    private static final Logger log = LoggerFactory.getLogger(ScheduleEventViewPredicate.class);

    Date stop;

    public ScheduleEventViewPredicate() {
        Collection<? extends GrantedAuthority> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        int daysToAdd = 3;
        for (GrantedAuthority role : roles) {
            String roleName = role.getAuthority();
            if (roleName != null) {
                switch (roleName) {
                    case "ROLE_API_USER":
                    case "ROLE_API_SUPERCLIENT":
                    case "ROLE_API_SUPERPROCESS":
                        daysToAdd = -1;
                        break;
                    case "ROLE_API_CLIENT":
                        daysToAdd = 3;
                        break;
                    default:
                        break;
                }
            }
        }
        if (daysToAdd > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, daysToAdd);
            stop = guideDayStart(calendar.getTime());
            log.debug("Setting stop to " + stop);
        }
    }

    @Override
    public boolean apply(@Nullable ScheduleEvent input) {
        return (stop == null || input.getStart().compareTo(stop) <= 0);
    }

    private static Date guideDayStart(Date guideDay) {
        DateTimeZone timeZone = DateTimeZone.forID("Europe/Amsterdam");
        DateTime dateTime = new DateTime(guideDay, timeZone);
        return dateTime.withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusDays(1).toDate();
    }

}
