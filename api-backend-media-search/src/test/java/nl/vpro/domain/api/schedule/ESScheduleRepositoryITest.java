package nl.vpro.domain.api.schedule;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.xml.bind.JAXB;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.domain.user.Broadcaster;

import static nl.vpro.domain.media.MediaBuilder.broadcast;
import static nl.vpro.domain.media.MediaBuilder.movie;
import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class ESScheduleRepositoryITest extends AbstractMediaESRepositoryITest {

    public ESScheduleRepository repository;

    @Override
    protected void firstRun() {
        createIndexIfNecessary(APIMEDIA);
    }

    @BeforeEach
    public void setup() {
        repository = new ESScheduleRepository(clientFactory,
            new ESMediaRepository(clientFactory, "", new MediaScoreManagerImpl()), new MediaScoreManagerImpl());
        repository.setIndexName(indexHelpers.get(APIMEDIA).getIndexName());
        repository.setScore(false);
        clearIndices();
    }

    @Test
    public void list() {
        index(broadcast().mid("DONNA_1")
            .scheduleEvents(
                event(Channel.BBC1, "2015-06-19T10:00:00"),
                event(Channel.BBC1, "2015-06-18T10:00:00")
            ));

        index(broadcast().mid("DONNA_2")
            .scheduleEvents(
                event(Channel.BBC2, "2015-06-19T10:00:00")
            ));


        ScheduleResult result = repository.listSchedules((Instant) null, null, Order.ASC, 0L, 10);
        assertThat(result).hasSize(3);
    }

    @Test
    public void listTestScrolled() {
        index(broadcast().mid("DONNA_1")
            .scheduleEvents(
                event(Channel.BBC1, "2015-06-19T10:00:00"),
                event(Channel.BBC1, "2015-06-18T10:00:00")
            ));

        index(broadcast().mid("DONNA_2")
            .scheduleEvents(
                event(Channel.BBC2, "2015-06-19T10:00:00")
            ));


        index(broadcast().mid("DONNA_DELETED")
            .workflow(Workflow.DELETED)
            .scheduleEvents(
                event(Channel.BBC2, "2015-06-19T10:01:00")
            ));


        ScheduleResult result = repository.listSchedules((Instant) null, null, Order.ASC, 0L, 100000);
        assertThat(result).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getTotalQualifier()).isEqualTo(Result.TotalQualifier.GREATER_THAN_OR_EQUAL_TO);

    }


    @Test
    public void listSchedulesWithChannel() {
        index(broadcast().mid("DONNA_1")
                .scheduleEvents(
                        event(Channel.BBC1, "2015-06-19T10:00:00"),
                        event(Channel.BBC1, "2015-06-18T10:00:00")
                ));

        index(broadcast().mid("DONNA_2")
                .scheduleEvents(
                        event(Channel.BBC2, "2015-06-19T10:00:00")
                ));


        ScheduleResult result = repository.listSchedules(
            Channel.BBC1,
            date("2015-06-19T00:00:00"),
            date("2015-06-20T00:00:00"), Order.ASC, 0L, 10);
        assertThat(result).hasSize(1);
    }

    @Test
    public void nowForChannel() {
        Instant now = LocalDateTime.of(2020, 4, 15, 20, 40).atZone(Schedule.ZONE_ID).toInstant();

        index(broadcast().mid("M_1")
                .scheduleEvents(
                        event(Channel.OPVO, "2020-04-15T20:20:00", Duration.ofMinutes(10)),
                        event(Channel.BBC1, "2020-04-15T20:35:00", Duration.ofMinutes(10))
                ));

        index(broadcast().mid("DONNA_2")
                .scheduleEvents(
                        event(Channel.BBC2, "2015-06-19T10:00:00")
                ));
        List<? extends ApiScheduleEvent> items = repository.listSchedules(Channel.OPVO, null, now, Order.DESC, 0L, 1).getItems();
        // TODO, doesn't fail.

        assertThat(items).hasSize(1);
    }

    @Test
    public void listSchedulesWithSubtitles() {
        index(MediaTestDataBuilder.broadcast().mid("SUBS_PROG_1").withDutchCaptions());
        index(MediaTestDataBuilder.group().mid("SUBS_GROUP_1").withDutchCaptions());
        index(MediaTestDataBuilder.segment().mid("SUBS_SEGMENT_1").withDutchCaptions());

        assertThat(repository.findByMid("SUBS_PROG_1").hasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_GROUP_1").hasSubtitles()).isTrue();
        assertThat(repository.findByMid("SUBS_SEGMENT_1").hasSubtitles()).isTrue();
    }

    @Test
    public void findByCrid() {
        String cridToFind = "crid://uitzending/1";
        //String cridToFind = "criduitzending1";
        index(broadcast().mid("DONNA_2").crids(cridToFind));

        assertThat(repository.load(cridToFind).getMid()).isEqualTo("DONNA_2");
        assertThat(repository.load("DONNA_2").getMid()).isEqualTo("DONNA_2");


    }

    @Test
    public void listSchedulesForBroadcaster() {
        index(broadcast().mid("DONNA_1")
                .broadcasters(new Broadcaster("VPRO"))
                .scheduleEvents(
                        event(Channel.BBC1, "2015-06-19T10:00:00"),
                        event(Channel.BBC1, "2015-06-18T10:00:00")
                ));

        index(broadcast().mid("DONNA_2")
                .broadcasters(new Broadcaster("VPRO"))
                .scheduleEvents(
                        event(Channel.BBC2, "2015-06-19T11:00:00")
                ));

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
     * This reproduces NPA-526
     */
    @Test
    public void listSchedulesForBroadcasterWithMax() {

        Instant now = Instant.now();
        Instant first = LocalDateTime.parse("2018-11-19T10:00:00").atZone(Schedule.ZONE_ID).toInstant();
        for (int i = 0 ; i < 40; i++) {
            ScheduleEvent e1 =   ScheduleEvent.builder().channel(Channel.BBC1).start(first.plus(Duration.ofHours(i))).duration(Duration.ofMinutes(20)).build();
            ScheduleEvent e2 =  ScheduleEvent.builder().channel(Channel.BBC2).start(first.plus(Duration.ofHours(i + 1))).rerun(true).duration(Duration.ofMinutes(20)).build();
            MediaBuilder.ProgramBuilder builder = broadcast().mid("MID_" + i)
                .broadcasters(i % 3 != 0 ? "VPRO" : "EO")
                .creationDate(now.plusMillis(i))
                .scheduleEvents(e1, e2)
                ;
            index(builder);
            Program p = builder.build();
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
    public void listSchedulesForMediaType() {
        MediaObject[] indexed = index(
            broadcast().mid("GEENMOVIE")
                .scheduleEvents(event(Channel.NED2, "2016-07-08T11:00:00")),
            movie().mid("MOVIE")
                .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00")));

        ScheduleResult result = repository.listSchedulesForMediaType(MediaType.MOVIE, date("2016-07-08T10:00:00"), date("2016-07-08T12:00:00"), Order.ASC, 0L, 10);

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getParent)).containsExactly((Program) indexed[1]);
    }


    @Test
    public void listSchedulesForAncestors() {
        MediaObject[] indexed = index(
            broadcast().mid("p1")
                .descendantOf("DESCENDANT1")
                .scheduleEvents(event(Channel.NED2, "2016-07-08T11:00:00")),
            movie().mid("p2")
                .descendantOf("DESCENDANT2")
                .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
        );


        ScheduleResult result = repository.listSchedulesForAncestor("DESCENDANT1",
            date("2016-07-08T10:00:00"), date("2016-07-08T12:00:00"), Order.ASC, 0L, 10);

        assertThat(result.getItems().stream().map(ApiScheduleEvent::getParent)).containsExactly((Program) indexed[0]);
    }


    @Test
    public void findSchedulesForRerun() {
        index(
            broadcast().mid("p1")
                .descendantOf("DESCENDANT1")
                .scheduleEvents(
                    event(Channel.NED2, "2016-07-08T11:00:00"),
                    rerun(Channel.NED2, "2016-07-08T14:00:00")
                ),
            movie().mid("p2")
                .descendantOf("DESCENDANT2")
                .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
        );
        ScheduleForm form = ScheduleForm.from(
            MediaForm.builder()
                .scheduleEvents(
                    ScheduleEventSearch.builder()
                        .begin(date("2016-07-08T00:00:00"))
                        .end(date("2016-07-09T00:00:00"))
                        .rerun(true)
                        .build()
                ).build());
        ScheduleSearchResult schedules = repository.findSchedules(null, form, null, 0L, 10);
        assertThat(schedules).hasSize(1);
    }



    @Test
    public void findSchedulesForOriginal() {

        index(
            broadcast()
                .mid("p1")
                .mainTitle("original and rerun on the same channel")
                .descendantOf("DESCENDANT1")
                .scheduleEvents(
                    event(Channel.NED2, "2016-07-08T11:00:00"),
                    rerun(Channel.NED2, "2016-07-08T14:00:00")
                ),
            movie().mid("p2")
                .descendantOf("DESCENDANT2")
                .mainTitle("original on a channel")
                .scheduleEvents(event(Channel.NED3, "2016-07-08T11:00:00"))
        );

        ScheduleForm form = ScheduleForm.from(
            MediaForm.builder()
                .scheduleEvents(
                    ScheduleEventSearch.builder()
                        .begin(date("2016-07-08T00:00:00"))
                        .end(date("2016-07-09T00:00:00"))
                        .rerun(false)
                        .build()
                ).build());
        ScheduleSearchResult schedules = repository.findSchedules(null, form, null, 0L, 10);
        assertThat(schedules).hasSize(2);

        assertThat(schedules.getItems().stream().map(se -> se.getResult().getMidRef())).containsExactly("p1", "p2");

    }
    @Test
    public void findScheduleWithGenre() {
        index(
            broadcast().mid("p1")
                .genres("3.0.1.2", "3.0.1.2.3")
                .scheduleEvents(
                    event(Channel.NDR3, "2018-01-23T11:00:00")
                ),
            broadcast().mid("p2")
                .scheduleEvents(
                    event(Channel.NDR3, "2018-01-23T12:00:00")
                )
        );

        String example = "<api:scheduleForm xmlns:api=\"urn:vpro:api:2013\" xmlns:media=\"urn:vpro:media:2009\" xmlns:pages=\"urn:vpro:pages:2013\">\n" +
            "  <api:searches>\n" +
            "    <api:genres match=\"MUST\">\n" +
            "      <api:matcher match=\"SHOULD\">3.0.1.2</api:matcher>\n" +
            "      <api:matcher matchType=\"WILDCARD\" match=\"SHOULD\">3.0.1.2.*</api:matcher>\n" +
            "    </api:genres>\n" +
            "    <api:scheduleEvents>\n" +
            "      <api:begin>2018-01-23T06:00:00+01:00</api:begin>\n" +
            "      <api:end>2018-01-24T06:00:00+01:00</api:end>\n" +
            "      <api:channel>NDR3</api:channel>\n" +
            "    </api:scheduleEvents>\n" +
            "  </api:searches>\n" +
            "</api:scheduleForm>";
        ScheduleForm form = JAXB.unmarshal(new StringReader(example), ScheduleForm.class);
        ScheduleSearchResult schedules = repository.findSchedules(null, form, null,0L, 10);
        assertThat(schedules).hasSize(1);
    }

    @Test
    public void illegalIfMoreScheduleEvents() {
        ScheduleForm form = ScheduleForm.from(
            MediaForm.builder()
                .scheduleEvents(
                    ScheduleEventSearch.builder()
                        .begin(date("2016-07-08T00:00:00"))
                        .end(date("2016-07-09T00:00:00"))
                        .rerun(true)
                        .build(),
                    ScheduleEventSearch.builder()
                        .begin(date("2017-07-08T00:00:00"))
                        .end(date("2017-07-09T00:00:00"))
                        .rerun(true)
                        .build()
                ).build());
        assertThatThrownBy(() ->
            repository.findSchedules(null, form, null, 0L, 10)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageFindingMatch(".+");

    }

    @Test
    void listSchedulesChannelGuideDay() {
        index(
            broadcast().mid("p1") // doesn't match
                .scheduleEvents(
                    event(Channel.NED3, "2018-01-23T11:00:00")
                ),
            broadcast().mid("p2") //  matches
                .scheduleEvents(
                    event(Channel.NED3, "2021-10-23T12:00:00")
                ),
            broadcast().mid("p3") // matches date on wrong channel, and channel on wrong date, so doesn't math
                .scheduleEvents(
                    event(Channel.NED2, "2021-10-23T12:00:00"),
                    event(Channel.NED3, "2020-10-23T12:00:00")
                ),
            broadcast().mid("p4") // matches in the night
                .scheduleEvents(
                    event(Channel.NED3, "2021-10-24T02:00:00")
                )

        );

        ScheduleResult apiScheduleEvents = repository.listSchedules(Channel.NED3,
            LocalDate.of(2021, 10, 23), Order.ASC, 0, 3);


        assertThat(apiScheduleEvents.stream().map(ScheduleEvent::getMidRef)).containsExactly("p2", "p4");

        //assertThat(apiScheduleEvents.getTotal()).isEqualTo(2); // FAILS NPA-599

    }

    private ScheduleEvent event(Channel c, String start, Duration duration) {
        return ScheduleEvent.builder()
            .channel(c)
            .start(date(start))
            .duration(duration)
            .build();
    }
    private ScheduleEvent event(Channel c, String start) {
        return event(c, start, Duration.ofHours(1));
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


    @SafeVarargs
    private final <MO extends MediaObject, B extends MediaBuilder<B, MO>> MediaObject[] index(B... os) {
        MediaObject[] result = new MediaObject[os.length];
        int i = 0;
        for (B o : os) {
            if (o.getWorkflow() != Workflow.DELETED) {
                o.workflow(Workflow.PUBLISHED);
            }
            MO program = o.build();
            result[i++] = program;
            indexHelpers.get(APIMEDIA).index(
                o.getMid(),
                program
            );
        }
        refresh();
        return result;

    }

}
