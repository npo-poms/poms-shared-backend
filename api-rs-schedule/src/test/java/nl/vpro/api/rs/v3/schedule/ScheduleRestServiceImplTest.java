package nl.vpro.api.rs.v3.schedule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Date;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import nl.vpro.api.rs.v3.AbstractRestServiceImplTest;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleSearchResult;
import nl.vpro.domain.api.media.ScheduleService;
import nl.vpro.domain.media.ScheduleEvent;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ScheduleRestServiceImplTest extends AbstractRestServiceImplTest<ScheduleRestServiceImpl> {

    final ScheduleService scheduleService = mock(ScheduleService.class);

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
        scheduleEvent.setStart(new Date(1000000));
        scheduleEvent.setDuration(new Date(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1000100));

        assertThat(eventActive).isTrue();
    }

    @Test
    public void inactiveEvent() throws Exception {
        ScheduleRestService scheduleRestService = getTestObject();

        Method method = scheduleRestService.getClass().getDeclaredMethod("isActiveEvent", ScheduleEvent.class, Date.class);
        method.setAccessible(true);

        ScheduleEvent scheduleEvent = new ScheduleEvent();
        scheduleEvent.setStart(new Date(1000000));
        scheduleEvent.setDuration(new Date(1000));

        Boolean eventActive = (Boolean) method.invoke(scheduleRestService, scheduleEvent, new Date(1002100));

        assertThat(eventActive).isFalse();
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


        when(scheduleService.find(any(MediaForm.class), anyString(), anyLong(), anyInt())).thenReturn(new ScheduleSearchResult());

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

            ArgumentCaptor<MediaForm> argument = ArgumentCaptor.forClass(MediaForm.class);

            verify(scheduleService).find(argument.capture(), anyString(), anyLong(), anyInt());
            assertThat(argument.getValue().getSearches().getScheduleEvents().getBegin().getTime()).isEqualTo(1435733201098l);
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

    @Override
    protected ScheduleRestServiceImpl getTestObject() {
        return new ScheduleRestServiceImpl(scheduleService);

    }

}
