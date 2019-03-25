/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.media.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author rico
 * @since 3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/nl/vpro/api/rs/v3/filter/scheduleEventViewSortedSetTest-context.xml")
public class ScheduleEventViewSortedSetTest {
    @Test
    public void testFilteredSetApiClient() {
        Collection<GrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user", "dontcare", (List<GrantedAuthority>)roles));

        Program program = MediaTestDataBuilder.program().withScheduleEvents().withMid().build();
        ZonedDateTime time = ZonedDateTime.now(Schedule.ZONE_ID).plusDays(4).withHour(10);
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, time.toInstant(), Duration.ofMillis(1000));
        program.getScheduleEvents().add(event);

        SortedSet<ScheduleEvent> events = program.getScheduleEvents();
        assertThat(events.size()).isEqualTo(5);

        ApiMediaFilter.set("scheduleEvents:4");
        FilteredSortedSet<ScheduleEvent> filteredEvents = FilteredSortedSet.wrap("scheduleEvents", events);
        assertThat(filteredEvents.size()).isEqualTo(4);
    }

    @Test
    public void testFilteredSetApiUser() {
        Collection<GrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user", "dontcare", (List<GrantedAuthority>)roles));

        Program program = MediaTestDataBuilder
            .program()
            .withScheduleEvents()
            .withMid()
            .withDescendantOf().build();
        ZonedDateTime time = ZonedDateTime.now(Schedule.ZONE_ID).plusDays(4).withHour(10);
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, time.toInstant(), Duration.ofMillis(1000));
        program.getScheduleEvents().add(event);

        SortedSet<ScheduleEvent> events = program.getScheduleEvents();
        assertThat(events.size()).isEqualTo(5);

        ApiMediaFilter.set("scheduleEvents:4");
        FilteredSortedSet<ScheduleEvent> filteredEvents = FilteredSortedSet.wrap("scheduleEvents", events);
        assertThat(filteredEvents.size()).isEqualTo(4);

        assertThat(filteredEvents.first().getParent().getDescendantOf()).isNotEmpty();
    }
}
