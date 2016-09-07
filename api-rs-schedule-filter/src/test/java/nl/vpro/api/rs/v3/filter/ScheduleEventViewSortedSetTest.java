/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.ScheduleEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 4);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, calendar.getTime(), new Date(1000));
        program.getScheduleEvents().add(event);

        SortedSet<ScheduleEvent> events = program.getScheduleEvents();
        assertThat(events.size()).isEqualTo(5);

        SortedSet<ScheduleEvent> filteredEvents = ScheduleEventView.wrap(events);
        assertThat(filteredEvents.size()).isEqualTo(4);

        /* Now warp the schedule Set to test for its predicate with a limit of 2 */
        assertThat(ScheduleEventView.wrap(events, 2).size()).isEqualTo(2);
    }

    @Test
    public void testFilteredSetApiUser() {
        Collection<GrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user", "dontcare", (List<GrantedAuthority>)roles));

        Program program = MediaTestDataBuilder.program().withScheduleEvents().withMid().build();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 4);
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, calendar.getTime(), new Date(1000));
        program.getScheduleEvents().add(event);

        SortedSet<ScheduleEvent> events = program.getScheduleEvents();
        assertThat(events.size()).isEqualTo(5);

        SortedSet<ScheduleEvent> filteredEvents = ScheduleEventView.wrap(events);
        assertThat(filteredEvents.size()).isEqualTo(5);
    }
}
