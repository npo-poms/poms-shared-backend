/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import nl.vpro.domain.media.*;

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

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author rico
 * @date 28/02/2014
 * @since 3.0
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/nl/vpro/api/rs/v3/filter/scheduleFilterTest-context.xml")
public class ScheduleFilterTest {

    @Autowired
    UserDetailsService userDetailsService;

    @Before
    public void init() {
        Collection roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user", "dontcare", (List<GrantedAuthority>)roles));
    }

    @Test
    @Ignore
    public void testFilter() {
        Program program = MediaTestDataBuilder.ProgramTestDataBuilder.program().withScheduleEvents().withMid().build();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 4);
        ScheduleEvent event = new ScheduleEvent(Channel.NED3, calendar.getTime(), new Date(1000));
        program.getScheduleEvents().add(event);

        assertThat(program.getScheduleEvents().size()).isEqualTo(5);

        Schedule schedule = new Schedule();
        schedule.addScheduleEventsFromMedia(program);

        assertThat(schedule.getScheduleEvents().size()).isEqualTo(4);
    }

}
