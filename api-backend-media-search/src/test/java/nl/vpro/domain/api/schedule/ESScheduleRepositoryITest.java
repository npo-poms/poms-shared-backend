package nl.vpro.domain.api.schedule;

import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.JAXB;

import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.es.ApiQueryIndex;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiMediaIndex;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ESScheduleRepositoryITest extends AbstractMediaESRepositoryITest {

    public ESScheduleRepository repository;


    @Override
    protected void firstRun() {
        createIndexIfNecessary(ApiMediaIndex.APIMEDIA);
    }

    @Before
    public void setup() {
        repository = new ESScheduleRepository(clientFactory, null);
        repository.setIndexName(indexNames.get(ApiMediaIndex.APIMEDIA));
        repository.setScore(false);
        clearIndices();
    }

    @Test
    public void list() throws Exception {
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
    public void listTestScrolled() throws Exception {
        index(MediaBuilder.program().mid("DONNA_1")
            .scheduleEvents(
                event(Channel.BBC1, "2015-06-19T10:00:00"),
                event(Channel.BBC1, "2015-06-18T10:00:00")
            ).build());

        index(MediaBuilder.program().mid("DONNA_2")
            .scheduleEvents(
                event(Channel.BBC2, "2015-06-19T10:00:00")
            ).build());


        ScheduleResult result = repository.listSchedules((Instant) null, null, Order.ASC, 0L, 100000);
        assertThat(result).hasSize(3);
    }


    @Test
    public void listSchedulesWithChannel() throws Exception {
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
    public void listSchedulesWithSubtitles() throws Exception {
        index(MediaTestDataBuilder.program().mid("SUBS_PROG_1").withDutchCaptions().build());
        index(MediaTestDataBuilder.group().mid("SUBS_GROUP_1").withDutchCaptions().build());
        index(MediaTestDataBuilder.segment().mid("SUBS_SEGMENT_1").withDutchCaptions().build());

        assertThat(repository.findByMid("SUBS_PROG_1").hasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_GROUP_1").hasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_SEGMENT_1").hasSubtitles()).isTrue();
    }

    @Test
    public void findByCrid() throws Exception {
        String cridToFind = "crid://uitzending/1";
        //String cridToFind = "criduitzending1";
        index(MediaBuilder.program().mid("DONNA_2").crids(cridToFind).build());

        assertThat(repository.load(cridToFind).getMid()).isEqualTo("DONNA_2");
        assertThat(repository.load("DONNA_2").getMid()).isEqualTo("DONNA_2");


    }


    @Test
    public void listSchedulesForBroadcaster() throws Exception {
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


    /**
     * This reproduces API-249
     */
    @Ignore("Fails, see API-249")
    @Test
    public void listSchedulesForBroadcasterWithMax() throws Exception {

        Instant now = Instant.now();
        Instant first = LocalDateTime.parse("2018-11-19T10:00:00").atZone(Schedule.ZONE_ID).toInstant();
        for (int i = 0 ; i < 40; i++) {
            ScheduleEvent e1 =   ScheduleEvent.builder().channel(Channel.BBC1).start(first.plus(Duration.ofHours(i))).duration(Duration.ofMinutes(20)).build();
            ScheduleEvent e2 =  ScheduleEvent.builder().channel(Channel.BBC2).start(first.plus(Duration.ofHours(i + 1))).rerun(true).duration(Duration.ofMinutes(20)).build();
            Program p = MediaBuilder.program().mid("MID_" + i)
                .broadcasters(i % 3 != 0 ? "VPRO" : "EO")
                .creationDate(now.plusMillis(i))
                .scheduleEvents(e1, e2)
                .build();
            index(p);
            if (p.getBroadcasters().contains(new Broadcaster("VPRO"))) {
                log.info("{}", e1);
                log.info("{}", e2);

            }

        }
        {
            ScheduleResult result = repository.listSchedulesForBroadcaster("VPRO", date("2018-11-19T09:00:00"), date("2018-11-19T18:01:00"), Order.ASC, 0L, 200);
            assertThat(result).hasSize(11);
            assertThat(result.getItems().get(0).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(1)));
            assertThat(result.getItems().get(0).getChannel()).isEqualTo(Channel.BBC1);

            assertThat(result.getItems().get(1).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(2)));
            assertThat(result.getItems().get(1).getChannel()).isEqualTo(Channel.BBC1);

            assertThat(result.getItems().get(2).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(2)));
            assertThat(result.getItems().get(2).getChannel()).isEqualTo(Channel.BBC2);


        }
        {

            ScheduleResult result = repository.listSchedulesForBroadcaster("VPRO", date("2018-11-19T09:00:00"), date("2018-11-19T18:01:00"), Order.ASC, 0L, 5);
            assertThat(result).hasSize(5);

            // This can only be correctly solved with parent/child queries or so (or very expensive queries)
            assertThat(result.getItems().get(0).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(1)));
            assertThat(result.getItems().get(0).getChannel()).isEqualTo(Channel.BBC1);

            assertThat(result.getItems().get(1).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(2)));
            assertThat(result.getItems().get(1).getChannel()).isEqualTo(Channel.BBC1);

            assertThat(result.getItems().get(2).getStartInstant()).isEqualTo(first.plus(Duration.ofHours(2)));
            assertThat(result.getItems().get(2).getChannel()).isEqualTo(Channel.BBC2);

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

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getParent)).containsExactly(movie);
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

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getParent)).containsExactly(broadcast);
    }


    @Test
    public void findSchedulesForRerun() throws Exception {
        Program broadcast = MediaBuilder.program().mid("p1")
            .descendantOf("DESCENDANT1")
            .type(ProgramType.BROADCAST)
            .scheduleEvents(
                event(Channel.NED2, "2016-07-08T11:00:00"),
                rerun(Channel.NED2, "2016-07-08T14:00:00")
            )
            .build();

        Program movie = MediaBuilder.program().mid("p2")
            .descendantOf("DESCENDANT2")
            .type(ProgramType.MOVIE)
            .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
            .build();

        index(broadcast);
        index(movie);

        ScheduleForm form = ScheduleForm.from(
            MediaForm.builder()
                .scheduleEvents(
                    ScheduleEventSearch.builder()
                        .begin(date("2016-07-08T00:00:00"))
                        .end(date("2016-07-09T00:00:00"))
                        .rerun(true)
                        .build()
                ).build());
        ScheduleSearchResult schedules = repository.findSchedules(null, form, 0L, 10);
        assertThat(schedules).hasSize(1);
    }



    @Test
    public void findSchedulesForOriginal() throws Exception {
        Program broadcast = MediaBuilder.program()
            .mid("p1")
            .mainTitle("original and rerun on the same channel")
            .descendantOf("DESCENDANT1")
            .type(ProgramType.BROADCAST)
            .scheduleEvents(
                event(Channel.NED2, "2016-07-08T11:00:00"),
                rerun(Channel.NED2, "2016-07-08T14:00:00")
            )
            .build();

        Program movie = MediaBuilder.program().mid("p2")
            .descendantOf("DESCENDANT2")
            .mainTitle("original on a channel")
            .type(ProgramType.MOVIE)
            .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
            .build();

        index(broadcast);
        index(movie);

        ScheduleForm form = ScheduleForm.from(
            MediaForm.builder()
                .scheduleEvents(
                    ScheduleEventSearch.builder()
                        .begin(date("2016-07-08T00:00:00"))
                        .end(date("2016-07-09T00:00:00"))
                        .rerun(false)
                        .build()
                ).build());
        ScheduleSearchResult schedules = repository.findSchedules(null, form, 0L, 10);
        assertThat(schedules).hasSize(2);

        assertThat(schedules.getItems().stream().map(se -> se.getResult().getMidRef())).containsExactly("p1", "p2");

    }
    @Test
    public void findScheduleWithGenre() throws JsonProcessingException {
        Program broadcast1 = MediaBuilder.program().mid("p1")
            .type(ProgramType.BROADCAST)
            .genres("3.0.1.2", "3.0.1.2.3")
            .scheduleEvents(
                event(Channel.NDR3, "2018-01-23T11:00:00")
            )
            .build();
        Program broadcast2 = MediaBuilder.program().mid("p2")
            .type(ProgramType.BROADCAST)
            .scheduleEvents(
                event(Channel.NDR3, "2018-01-23T12:00:00")
            )
            .build();
        index(broadcast1, broadcast2);

        String example = "<api:scheduleForm xmlns:api=\"urn:vpro:api:2013\" xmlns:media=\"urn:vpro:media:2009\" xmlns:pages=\"urn:vpro:pages:2013\"><api:searches><api:genres match=\"MUST\"><api:matcher match=\"SHOULD\">3.0.1.2</api:matcher><api:matcher matchType=\"WILDCARD\" match=\"SHOULD\">3.0.1.2.*</api:matcher></api:genres><api:scheduleEvents><api:begin>2018-01-23T06:00:00+01:00</api:begin><api:end>2018-01-24T06:00:00+01:00</api:end><api:channel>NDR3</api:channel></api:scheduleEvents></api:searches></api:scheduleForm>";
        ScheduleForm form = JAXB.unmarshal(new StringReader(example), ScheduleForm.class);
        ScheduleSearchResult schedules = repository.findSchedules(null, form, 0L, 10);
        assertThat(schedules).hasSize(1);
    }

    private ScheduleEvent event(Channel c, String start) {
        return ScheduleEvent.builder()
            .channel(c)
            .start(date(start))
            .duration(Duration.ofHours(1))
            .build();
    }

    private ScheduleEvent rerun(Channel c, String start) {
        return ScheduleEvent.builder()
            .channel(c)
            .start(date(start))
            .duration(Duration.ofHours(1))
            .repeat(Repeat.rerun())
            .build();
    }

    private Instant date(String s) {
        return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(Schedule.ZONE_ID).toInstant();
    }

    private void index(Program... ps) throws JsonProcessingException {
        for (Program p : ps) {
            index(p, "program");
        }
    }

    private void index(Segment s) throws Exception {
        index(s, "segment");
    }

    private void index(Group g) throws JsonProcessingException {
        index(g, "group");
    }

    private void index(MediaObject o, String mediaType) throws JsonProcessingException {
        client.prepareIndex()
                .setIndex(indexNames.get(ApiQueryIndex.INSTANCE))
                .setType(mediaType)
                .setId(o.getMid())
                .setSource(Jackson2Mapper.getPublisherInstance().writeValueAsBytes(o), XContentType.JSON)
                .get();
        refresh();

    }

}
