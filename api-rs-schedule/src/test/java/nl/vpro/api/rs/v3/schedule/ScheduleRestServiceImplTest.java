package nl.vpro.api.rs.v3.schedule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import nl.vpro.api.rs.v3.AbstractRestServiceImplTest;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.Schedule;
import nl.vpro.domain.media.ScheduleEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ScheduleRestServiceImplTest extends AbstractRestServiceImplTest<ScheduleRestServiceImpl> {

    private final ScheduleService scheduleService = mock(ScheduleService.class);

    @After
    public void after() {
        reset(scheduleService);
    }

    @Test
    public void activeEvent() throws Exception {

        ScheduleRestService scheduleRestService = getTestObject();
        Method method = scheduleRestService.getClass().getDeclaredMethod("isActiveEvent", ScheduleEvent.class, Date.class);
        method.setAccessible(true);

        ScheduleEvent scheduleEvent = new ScheduleEvent();
        scheduleEvent.setStartInstant(Instant.ofEpochMilli(1000000));
        scheduleEvent.setDuration(Duration.ofMillis(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1000100));

        assertThat(eventActive).isTrue();
    }

    @Test
    public void inactiveEvent() throws Exception {
        ScheduleRestService scheduleRestService = getTestObject();

        Method method = scheduleRestService.getClass().getDeclaredMethod("isActiveEvent", ScheduleEvent.class, Date.class);
        method.setAccessible(true);

        ScheduleEvent scheduleEvent = new ScheduleEvent();
        scheduleEvent.setStartInstant(Instant.ofEpochMilli(1000000));
        scheduleEvent.setDuration(Duration.ofMillis(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1002100));

        assertThat(eventActive).isFalse();
    }

    @Test
    public void testGuideDayStartStop() throws ParseException {

        ScheduleRestService scheduleRestService = getTestObject();
        Result res = scheduleRestService.listChannel(Channel.KETN.name(), LocalDate.of(2015, 3, 28), null, null, null, "ASC", 0L, 100);
        Instant start = ZonedDateTime.parse("2015-03-28T06:00:00+01:00").toInstant();
        Instant stop = ZonedDateTime.parse("2015-03-29T06:00:00+02:00").toInstant();

        verify(scheduleService).list(Channel.KETN, start, stop, Order.ASC, 0L, 100);
    }


    @Test
    public void testStartStop() throws ParseException {


        ScheduleRestService scheduleRestService = getTestObject();
        Result res = scheduleRestService.listChannel(Channel.KETN.name(), null, LocalDate.of(2016, 3, 5).atTime(12, 34).atZone(Schedule.ZONE_ID).toInstant(), null, null, "ASC", 0L, 100);
        Instant start = ZonedDateTime.parse("2016-03-05T12:34:00+01:00").toInstant();

        verify(scheduleService).list(Channel.KETN, start, null, Order.ASC, 0L, 100);
    }

    @Test
    public void testStartStopRequest() throws URISyntaxException {
        when(scheduleService.list(any(Channel.class), any(Instant.class), any(Instant.class), any(Order.class), anyLong(), anyInt())).thenReturn(new ScheduleResult());

        MockHttpRequest request = MockHttpRequest.get("/schedule/channel/KETN?start=2016-03-05T06:00&max=100");
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assert200(response);

        Instant start = ZonedDateTime.parse("2016-03-05T06:00:00+01:00").toInstant();
        verify(scheduleService).list(Channel.KETN, start, null, Order.ASC, 0L, 100);
    }

    private String MSE_2775form() {
        return "{\n" +
            "  \"searches\" : {\n" +
            "    \"broadcasters\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : \"MAX\"\n" +
            "    },\n" +
            "    \"types\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : [ {\n" +
            "        \"value\" : \"BROADCAST\",\n" +
            "        \"match\" : \"SHOULD\"\n" +
            "      }, {\n" +
            "        \"value\" : \"SEGMENT\",\n" +
            "        \"match\" : \"SHOULD\"\n" +
            "      } ]\n" +
            "    },\n" +
            "    \"avTypes\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : \"VIDEO\"\n" +
            "    },\n" +
            "    \"scheduleEvents\" : {\n" +
            "      \"channel\" : \"NED1\",\n" +
            "      \"begin\" : \"2015-07-01T06:46:41.098Z\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"sort\" : {\n" +
            "    \"sortDate\" : \"ASC\"\n" +
            "  }\n" +
            "}\n";
    }

    private String MSE2775_formCorrected() {
        return "{\n" +
            "  \"searches\" : {\n" +
            "    \"broadcasters\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : \"MAX\"\n" +
            "    },\n" +
            "    \"types\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : [ {\n" +
            "        \"value\" : \"BROADCAST\",\n" +
            "        \"match\" : \"SHOULD\"\n" +
            "      }, {\n" +
            "        \"value\" : \"SEGMENT\",\n" +
            "        \"match\" : \"SHOULD\"\n" +
            "      } ]\n" +
            "    },\n" +
            "    \"avTypes\" : {\n" +
            "      \"match\" : \"MUST\",\n" +
            "      \"value\" : \"VIDEO\"\n" +
            "    },\n" +
            "    \"scheduleEvents\" : {\n" +
            "      \"channel\" : \"NED1\",\n" +
            "      \"begin\" : 1435733201098\n" +
            "    }\n" +
            "  },\n" +
            "  \"sort\" : {\n" +
            "    \"sortDate\" : \"ASC\"\n" +
            "  }\n" +
            "}\n";
    }

    private void MSE_2775(String form) throws URISyntaxException, IOException {


        when(scheduleService.find(any(ScheduleForm.class), anyString(), anyLong(), anyInt())).thenReturn(new ScheduleSearchResult());

        {
            MockHttpRequest request = MockHttpRequest.post("/schedule");
            request.contentType(MediaType.APPLICATION_JSON);
            request.accept(MediaType.APPLICATION_JSON);

            MockHttpResponse response = new MockHttpResponse();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(form.getBytes());
            request.content(out.toByteArray());

            dispatcher.invoke(request, response);
            assertThat(response.getStatus()).isEqualTo(200);

            ArgumentCaptor<ScheduleForm> argument = ArgumentCaptor.forClass(ScheduleForm.class);

            verify(scheduleService).find(argument.capture(), anyString(), anyLong(), anyInt());
            assertThat(argument.getValue().getSearches().getScheduleEvents().getBegin().getTime()).isEqualTo(1435733201098L);
        }
    }
    @Test
    public void MSE_2775_1() throws IOException, URISyntaxException {
        MSE_2775(MSE_2775form());
    }

    @Test
    public void MSE_2775_2() throws IOException, URISyntaxException {
        MSE_2775(MSE2775_formCorrected());

    }

    @Test
    public void NPA_202_scheduleform() throws URISyntaxException {
        when(scheduleService.find(any(ScheduleForm.class), anyString(), anyLong(), anyInt())).thenReturn(new ScheduleSearchResult());

        MockHttpRequest request = MockHttpRequest.post("/schedule");
        request.contentType(MediaType.APPLICATION_XML);
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(new ScheduleForm(), out);
        request.content(out.toByteArray());

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);

    }

    @Test
    public void NPA_202_mediaform() throws URISyntaxException {
        when(scheduleService.find(any(ScheduleForm.class), anyString(), anyLong(), anyInt())).thenReturn(new ScheduleSearchResult());

        MockHttpRequest request = MockHttpRequest.post("/schedule");
        request.contentType(MediaType.APPLICATION_XML);
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MediaForm form = new MediaForm();
        form.setSearches(new MediaSearch());

        JAXB.marshal(form, out);

        System.out.println(new String(out.toByteArray()));

        request.content(out.toByteArray());

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Override
    protected ScheduleRestServiceImpl getTestObject() {
        return new ScheduleRestServiceImpl(scheduleService);
    }
}
