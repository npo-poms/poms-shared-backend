package nl.vpro.domain.api.schedule;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.ScheduleResult;
import nl.vpro.domain.media.*;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.elasticsearch.LocalClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Assertions.assertThat;


public class ESScheduleRepositoryTest  {

    protected LocalClientFactory clientFactory = new LocalClientFactory();
    protected Client client;

    public ESScheduleRepository repository;

    @Before
    public void setup() throws IOException, ExecutionException, InterruptedException {
        client = clientFactory.client("test");
        repository = new ESScheduleRepository(clientFactory);
        repository.setIndexName("media");
        repository.createIndices().get();
    }

    @Test
    public void list() throws JsonProcessingException {
        index(MediaBuilder.program().mid("DONNA_1")
            .scheduleEvents(
                event(Channel.BBC1, "2015-06-19T10:00:00"),
                event(Channel.BBC1, "2015-06-18T10:00:00")
            ).build());

        index(MediaBuilder.program().mid("DONNA_2")
            .scheduleEvents(
                event(Channel.BBC2, "2015-06-19T10:00:00")
            ).build());


        ScheduleResult result = repository.listSchedules((Instant) null, null, Order.ASC, 0L, 10);
        assertThat(result).hasSize(3);
    }


    @Test
    public void listSchedulesWithChannel() throws JsonProcessingException {
        index(MediaBuilder.program().mid("DONNA_1")
                .scheduleEvents(
                        event(Channel.BBC1, "2015-06-19T10:00:00"),
                        event(Channel.BBC1, "2015-06-18T10:00:00")
                ).build());

        index(MediaBuilder.program().mid("DONNA_2")
                .scheduleEvents(
                        event(Channel.BBC2, "2015-06-19T10:00:00")
                ).build());


        ScheduleResult result = repository.listSchedules(Channel.BBC1, date("2015-06-19T00:00:00"), date("2015-06-20T00:00:00"), Order.ASC, 0L, 10);
        assertThat(result).hasSize(1);
    }

    @Test
    public void listSchedulesWithSubtitles() throws JsonProcessingException {
        index(MediaTestDataBuilder.program().mid("SUBS_PROG_1").withDutchCaptions().build());
        index(MediaTestDataBuilder.group().mid("SUBS_GROUP_1").withDutchCaptions().build());
        index(MediaTestDataBuilder.segment().mid("SUBS_SEGMENT_1").withDutchCaptions().build());

        assertThat(repository.findByMid("SUBS_PROG_1").isHasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_GROUP_1").isHasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_SEGMENT_1").isHasSubtitles()).isTrue();
    }

    @Test
    public void findByCrid() throws IOException {
        String cridToFind = "crid://uitzending/1";
        //String cridToFind = "criduitzending1";
        index(MediaBuilder.program().mid("DONNA_2").crids(cridToFind).build());

        assertThat(repository.load(cridToFind).getMid()).isEqualTo("DONNA_2");
        assertThat(repository.load("DONNA_2").getMid()).isEqualTo("DONNA_2");

    }


    @Test
    public void listSchedulesForBroadcaster() throws JsonProcessingException {
        index(MediaBuilder.program().mid("DONNA_1")
                .broadcasters(new Broadcaster("VPRO"))
                .scheduleEvents(
                        event(Channel.BBC1, "2015-06-19T10:00:00"),
                        event(Channel.BBC1, "2015-06-18T10:00:00")
                ).build());

        index(MediaBuilder.program().mid("DONNA_2")
                .broadcasters(new Broadcaster("VPRO"))
                .scheduleEvents(
                        event(Channel.BBC2, "2015-06-19T11:00:00")
                ).build());

        {
            ScheduleResult result = repository.listSchedulesForBroadcaster("VPRO", date("2015-06-19T00:00:00"), date("2015-06-20T00:00:00"), Order.ASC, 0L, 10);
            assertThat(result).hasSize(2);
            assertThat(result.getItems().get(0).getChannel()).isEqualTo(Channel.BBC1);
            assertThat(result.getItems().get(1).getChannel()).isEqualTo(Channel.BBC2);
        }
        {  // Test order too
            ScheduleResult result = repository.listSchedulesForBroadcaster("VPRO", date("2015-06-19T00:00:00"), date("2015-06-20T00:00:00"), Order.DESC, 0L, 10);
            assertThat(result).hasSize(2);
            assertThat(result.getItems().get(0).getChannel()).isEqualTo(Channel.BBC2);
            assertThat(result.getItems().get(1).getChannel()).isEqualTo(Channel.BBC1);
        }
    }

    @Test
    public void listSchedulesForMediaType() throws Exception {
        Program broadcast = MediaBuilder.program().mid("GEENMOVIE")
                .type(ProgramType.BROADCAST)
                .scheduleEvents(event(Channel.NED2, "2016-07-08T11:00:00"))
                .build();

        Program movie = MediaBuilder.program().mid("MOVIE")
                .type(ProgramType.MOVIE)
                .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
                .build();

        index(broadcast);
        index(movie);

        ScheduleResult result = repository.listSchedulesForMediaType(MediaType.MOVIE, date("2016-07-08T10:00:00"), date("2016-07-08T12:00:00"), Order.ASC, 0L, 10);

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getMediaObject)).containsExactly(movie);
    }


    @Test
    public void listSchedulesForAncestors() throws Exception {
        Program broadcast = MediaBuilder.program().mid("p1")
            .descendantOf("DESCENDANT1")
            .type(ProgramType.BROADCAST)
            .scheduleEvents(event(Channel.NED2, "2016-07-08T11:00:00"))
            .build();

        Program movie = MediaBuilder.program().mid("p2")
            .descendantOf("DESCENDANT2")
            .type(ProgramType.MOVIE)
            .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
            .build();

        index(broadcast);
        index(movie);

        ScheduleResult result = repository.listSchedulesForAncestor("DESCENDANT1",
            date("2016-07-08T10:00:00"), date("2016-07-08T12:00:00"), Order.ASC, 0L, 10);

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getMediaObject)).containsExactly(broadcast);
    }

    private ScheduleEvent event(Channel c, String start) {
        ScheduleEvent event = new ScheduleEvent();
        event.setChannel(c);
        event.setStartInstant(date(start));
        event.setDuration(Duration.ofMillis(3600000));
        return event;
    }
    private Instant date(String s) {
        return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(Schedule.ZONE_ID).toInstant();
    }

    private void index(Program p) throws JsonProcessingException {
        index(p, "program");
    }

    private void index(Segment s) throws JsonProcessingException {
        index(s, "segment");
    }

    private void index(Group g) throws JsonProcessingException {
        index(g, "group");
    }

    private void index(MediaObject o, String mediaType) throws JsonProcessingException {
        client.prepareIndex()
                .setIndex("media")
                .setType(mediaType)
                .setId(o.getMid())
                .setSource(Jackson2Mapper.getInstance().writeValueAsBytes(o))
                .get();
        client.admin()
                .indices()
                .refresh(new RefreshRequest("media"))
                .actionGet();
    }
}
