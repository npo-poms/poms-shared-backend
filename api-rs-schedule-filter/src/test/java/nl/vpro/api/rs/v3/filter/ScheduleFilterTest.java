/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.media.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author rico
 * @since 3.0
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/nl/vpro/api/rs/v3/filter/scheduleFilterTest-context.xml")
public class ScheduleFilterTest {

    @Autowired
    UserDetailsService userDetailsService;

    @Before
    public void init() {
        Collection roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user", "dontcare", (List<GrantedAuthority>)roles));
    }

    @Test
    @Ignore("See nl.vpro.api.rs.v3.filter.ScheduleEventViewSortedSetTest for how it's done.")
    public void testFilter() {
        Program program = MediaTestDataBuilder.program().withScheduleEvents().withMid().build();
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, Instant.now().plus(Duration.ofDays(4)), Duration.ofMillis(1000));
        program.getScheduleEvents().add(event);

        assertThat(program.getScheduleEvents().size()).isEqualTo(5);

        Schedule schedule = new Schedule();
        schedule.addScheduleEventsFromMedia(program);

        assertThat(schedule.getScheduleEvents().size()).isEqualTo(4);
    }

}
