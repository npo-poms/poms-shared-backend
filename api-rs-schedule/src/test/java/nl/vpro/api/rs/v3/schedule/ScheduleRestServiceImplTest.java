package nl.vpro.api.rs.v3.schedule;

import nl.vpro.domain.api.media.ScheduleService;
import nl.vpro.domain.media.ScheduleEvent;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ScheduleRestServiceImplTest {

    @Test
    public void activeEvent() throws Exception {
        ScheduleService scheduleService = mock(ScheduleService.class);
        ScheduleRestService scheduleRestService = new ScheduleRestServiceImpl(scheduleService);

        Method method = scheduleRestService.getClass().getDeclaredMethod("isActiveEvent", ScheduleEvent.class, Date.class);
        method.setAccessible(true);

        ScheduleEvent scheduleEvent = new ScheduleEvent();
        scheduleEvent.setStart(new Date(1000000));
        scheduleEvent.setDuration(new Date(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1000100));

        assertThat(eventActive).isTrue();
    }

    @Test
    public void inactiveEvent() throws Exception {
        ScheduleService scheduleService = mock(ScheduleService.class);
        ScheduleRestService scheduleRestService = new ScheduleRestServiceImpl(scheduleService);

        Method method = scheduleRestService.getClass().getDeclaredMethod("isActiveEvent", ScheduleEvent.class, Date.class);
        method.setAccessible(true);

        ScheduleEvent scheduleEvent = new ScheduleEvent();
        scheduleEvent.setStart(new Date(1000000));
        scheduleEvent.setDuration(new Date(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1002100));

        assertThat(eventActive).isFalse();
    }
}