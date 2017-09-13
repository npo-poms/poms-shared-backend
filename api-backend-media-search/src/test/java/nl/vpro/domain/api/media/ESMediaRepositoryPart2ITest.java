package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.Settings;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.media.AVFileExtensionConstraint;
import nl.vpro.domain.constraint.media.BroadcasterConstraint;
import nl.vpro.domain.constraint.media.Filter;
import nl.vpro.domain.constraint.media.HasPredictionConstraint;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.util.FilteringIterator;

import static nl.vpro.domain.api.media.MediaFormBuilder.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * These are integration tests where the index is build in the @BeforeClass. Test which don't need that are placed in {@link ESMediaRepositoryPart1ITest}
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */

@Slf4j
public class ESMediaRepositoryPart2ITest extends AbstractMediaESRepositoryITest {

    private static final Instant NOW = LocalDate.of(2016, Month.JULY, 24).atTime(20, 0).atZone(Schedule.ZONE_ID).toInstant();
    private static final Instant LONGAGO = LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay().atZone(Schedule.ZONE_ID).toInstant();

    private static final MediaTestDataBuilder.ProgramTestDataBuilder programBuilder = MediaTestDataBuilder
        .program()
        .lastPublished(NOW)
        .workflow(Workflow.PUBLISHED)
        .constrained()
        .withImages();

    private static final MediaTestDataBuilder.GroupTestDataBuilder groupBuilder = MediaTestDataBuilder
        .group()
        .constrained()
        .lastPublished(NOW)
        .mid("POMS_S_12345")
        .type(GroupType.SERIES);

    private static final ESMediaRepository target = new ESMediaRepository((s) -> client, "tags");


    private static Group group;
    private static Group group_ordered;

    private static Program program1;
    private static Program program2;
    private static Program program3;

    private static Group sub_group;
    private static Program sub_program1;
    private static Program sub_program2;


    private static String[] testTags = {"Onderkast", "Bovenkast", "Geen kast", "Hoge kast", "Lage kast"};

    static List<MediaObject> indexed = new ArrayList<>();

    static int indexedObjectCount = 0;
    static int indexedProgramCount = 0;
    static int indexedGroupCount = 0;

    static Set<String> mids = new TreeSet<>();
    static int deletedObjectCount = 0;
    static int deletedProgramCount = 0;
    static int deletedGroupCount = 0;


    /**
     * Builds a test database
     */
    @Override
    protected void firstRun() throws InterruptedException, ExecutionException, IOException {

        createIndexIfNecessary(ApiMediaIndex.NAME);

        target.mediaRepository = mock(MediaRepository.class);
        target.settings = new Settings();
        when(target.mediaRepository.redirect(anyString())).thenReturn(Optional.empty());

        target.setIndexName(indexName);

        group = index(groupBuilder.published().build());
        group_ordered = index(MediaTestDataBuilder.group().constrained().published(NOW).type(GroupType.SERIES).withMid().build());
        // 2 groups
        program1 = index(programBuilder.copy()
            .publishStart(LocalDateTime.of(2017, 1, 30, 0, 0)) // sortDate is relevant for listDescendants
            .memberOf(group, 1)
            .memberOf(group_ordered, 7).episodeOf(group, 3).build());
        program2 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 29, 0, 0))
            .published(NOW).withMid()
            .memberOf(group_ordered, 2).build());
        program3 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 28, 0, 0))
            .published(NOW)
            .withMid()
            .memberOf(group_ordered, 3).build());
        sub_group = index(MediaTestDataBuilder.group().published()
            .mid("sub_group")
            .memberOf(group_ordered, 4)
            .published(NOW)
            .creationDate(NOW)
            .build());
        sub_program1 = index(programBuilder.copy()
            .publishStart(LocalDateTime.of(2017, 1, 30, 1, 0)) // sortDate is relevant for listDescendants
            .mid("sub_program_1")
            .published(NOW)
            .creationDate(NOW)
            .memberOf(sub_group, 1)
            .build());
        sub_program2 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 29, 2, 0))
            .mid("sub_program_2")
            .published(NOW)
            .creationDate(NOW)
            .memberOf(sub_group, 2).build());

        // order of descendant of group_ordered by sortDate should be
        // program3, program2, sub_program2, program1, sub_program1/sub_group
        // by member logical would be
        // program2, program3, sub_group, sub_program1, sub_program2, program1
        // or
        // program2, program3, sub_group, program1, sub_program1, sub_program2
        //3 + 2 programs (broadcasts), 1 sub group


        index(MediaTestDataBuilder.group().constrained().type(GroupType.COLLECTION).mid("VPGROUP_D1").lastPublished(NOW).workflow(Workflow.DELETED).title("Deleted Group").build());
        // 1 deleted group
        index(MediaTestDataBuilder.program().constrained().type(ProgramType.CLIP).mid("VPPROGRAM_D").lastPublished(NOW).workflow(Workflow.REVOKED).title("Deleted Program").build());
        index(MediaTestDataBuilder.program().constrained().type(ProgramType.CLIP).mid("VPPROGRAM_D1").lastPublished(NOW).workflow(Workflow.MERGED).title("Deleted Merged Program").mergedTo(program1).build());
        // 2 deleted programs

        for (int i = 0; i < 10; i++) {
            Group g = index(MediaTestDataBuilder.group()
                .constrained()
                .workflow(Workflow.PUBLISHED)
                .broadcasters("OMROEP" + (i % 3))
                .creationDate(LONGAGO.plusSeconds(i))
                .lastPublished(LONGAGO.plusSeconds(i * 2))
                .withGenres()
                .mid("MID_G_" + i)
                .build());
            index(MediaTestDataBuilder.program()
                .constrained()
                .workflow(Workflow.PUBLISHED)
                .creationDate(LONGAGO.plusSeconds(i))
                .lastPublished(LONGAGO.plusSeconds(i * 2 + 1))
                .duration(Duration.ofMillis(i * 100))
                .scheduleEvent(Channel.values()[i % 4], NOW.plus(Duration.ofHours(i)), Duration.ofMinutes(30)) // determins sortDate too!
                .tags("Tag " + (i % 3), testTags[i % testTags.length])
                .locations("http://domain.com/path.mp" + i)
                .withGenres()
                .broadcasters("OMROEP" + (i % 3))
                .memberOf(g, 1)
                .avType(AVType.values()[i % AVType.values().length])
                .predictions(Platform.INTERNETVOD)
                .mid("MID-" + i)
                .build());
        }
        // 10 groups, and 10 programs (broadcasts)

        // totals now 30 objects, 13 groups, 15 broadcasts (from which 3 deleted)


        // index some variations
        index(programBuilder.copy().mid("MID_HIGH_SCORE").mainTitle("This should give us a high score").build());
        // 29 objects, 14 programs


        index(groupBuilder.copy().mid("MID_SCORING_ON_DESCRIPTION")
                .mainDescription("While scoring a hit on a description field is likely to receive a much lower score then a hit on a title.").build());

        // 30, 13 groups

        Location wm = new Location("http://somedomain.com/path/to/file", OwnerType.BROADCASTER);
        wm.setAvFileFormat(AVFileFormat.WM);

        index(programBuilder.copy().mid("MID_WITH_LOCATION").locations(wm).build());

        index(programBuilder.copy().mid("MID_DRENTHE").broadcasters(new Broadcaster("TVDRENTHE", "TVDrenthe")).build());

        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        index(programBuilder.copy().mid("MID_WITH_RELATIONS").mainTitle("About Kubrick").relations(new Relation(director, "", "Stanley Kubrick")).build());

        // 33 (3 deleted)
        refresh();

        Map<String, String> redirects = new HashMap<>();
        when(target.mediaRepository.redirects()).thenReturn(new RedirectList(null, null, redirects));

        assertThat(indexedObjectCount).isEqualTo(indexedGroupCount + indexedProgramCount);
        assertThat(deletedObjectCount).isEqualTo(deletedGroupCount + deletedProgramCount);
    }


    @Test
    public void testGroupBy() {

        SearchResponse response = client.prepareSearch("apimedia")
            .addAggregation(AggregationBuilders.terms("types").field("_type").order(Terms.Order.term(true)))
            .setSize(0)
            .get();

        Terms a = response.getAggregations().get("types");
        String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
        assertThat(result).isEqualTo("deletedgroup:1,deletedprogram:2,group:14,program:19");
    }

    @Test
    public void testLoad() throws Exception {
        Program in = programBuilder.build();
        MediaObject result = target.load(in.getMid());

        assertThat(result).isEqualTo(in);
    }

    @Test
    public void testLoadAll() throws IOException {
        List<MediaObject> results = target.loadAll(Arrays.asList("MID-1", "BESTAATNIET", "MID-2"));
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).isNotNull();
        assertThat(results.get(1)).isNull();
        assertThat(results.get(2)).isNotNull();

    }

    @Test
    public void testList() {
        MediaResult results = target.list(Order.ASC, 0L, 1000);
        assertThat(results).hasSize(indexedObjectCount);
    }

    @Test
    public void testListWithOffset() {
        MediaResult results = target.list(Order.ASC, 10L, 1000);
        assertThat(results).hasSize(indexedObjectCount - 10);

    }

    @Test
    public void testChanges() throws IOException {
        Iterator<Change> changes = target.changes(LONGAGO.minus(1, ChronoUnit.SECONDS), null, null, null, Order.ASC, Integer.MAX_VALUE, null, null);
        List<Change> list = new ArrayList<>();
        changes.forEachRemaining(list::add);
        assertThat(list.stream().filter(Change::isDeleted).collect(Collectors.toList())).hasSize(3);
        assertThat(list).hasSize(indexedObjectCount + deletedObjectCount);
    }

    @Test
    public void testChangesSince() throws IOException {
        Iterator<Change> changes = target.changes(NOW.minus(1, ChronoUnit.SECONDS), null, null, null, Order.DESC, Integer.MAX_VALUE, null, null);
        List<Change> list = new ArrayList<>();
        changes.forEachRemaining(list::add);
        assertThat(list).hasSize(indexedObjectCount - 17); // 17 objects created around EPOCH
        assertThat(list.stream().filter(Change::isDeleted).collect(Collectors.toList())).hasSize(3);
    }

    @Test
    public void testChangesWithMax() throws IOException {
        Iterator<Change> changes = target.changes(Instant.EPOCH, null, null, null, Order.DESC, 10, null, null);
        List<Change> list = new ArrayList<>();
        changes.forEachRemaining(list::add);
        assertThat(list).hasSize(10);
    }

    @Test
    public void testIterate() {
        target.iterateBatchSize = 10;
        Iterator<MediaObject> results = target.iterate(null, null, 0L, 1000, FilteringIterator.noKeepAlive());
        assertThat(results).hasSize(indexedObjectCount);
    }

    @Test
    public void testIterateWithOffset() {
        target.iterateBatchSize = 10;
        Iterator<MediaObject> results = target.iterate(null, null, 10L, 1000, FilteringIterator.noKeepAlive());
        assertThat(results).hasSize(indexedObjectCount - 10);
    }


    @Test
    public void testLoadNotFound() throws Exception {
        MediaObject object = target.findByMid("bestaatniet");

        assertThat(object).isNull();
    }

    @Test
    public void testFindAll() throws Exception {
        SearchResult<MediaObject> result = target.find(null, null, 0L, 100);
        assertThat(result.asList().stream().map(MediaObject::getMid).sorted()).containsExactlyElementsOf(mids);

        assertThat(result.getTotal()).isEqualTo(indexedObjectCount);

    }

    @Test
    public void testFind() throws Exception {
        SearchResult<MediaObject> result = target.find(null, null, 2L, 5);


        assertThat(result.getTotal()).isEqualTo(indexedObjectCount);
        assertThat(result.getOffset()).isEqualTo(2);
        assertThat(result.getMax()).isEqualTo(5);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getItems()).hasSize(5);
    }


    @Test
    public void testFindOnProfileWithTextScore() throws Exception {
        MediaForm form = form().text("Text with Score words").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getItems().get(0).getResult()).isEqualTo(programBuilder.build());
        assertThat(result.getItems().get(1).getResult()).isEqualTo(groupBuilder.build());
    }

    @Test
    public void testFindWithWithMidMediaId() throws Exception {
        MediaForm form = form().mediaIds(programBuilder.build().getMid()).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithWithMidMediaIds() throws Exception {
        MediaForm form = form().mediaIds("MID-1", "MID-2").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    public void testFindWithWithSortDate() throws Exception {
        MediaForm form = form().asc(MediaSortField.sortDate).sortDate(NOW,  NOW.plus(Duration.ofHours(2))).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(2);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i).getResult().getSortDate()).isAfterOrEqualsTo(result.getItems().get(i - 1).getResult().getSortDate());
        }
    }


    @Test
    public void testFindWithWithPublishDateAsc() throws Exception {
        MediaForm form = form()
            .publishDate(LONGAGO, LONGAGO.plusSeconds(5))
            .sortOrder(MediaSortOrder.asc(MediaSortField.publishDate))
            .build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(5);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i).getResult().getLastPublished()).isAfter(result.getItems().get(i - 1).getResult().getLastPublished());
        }
    }


    @Test
    public void testFindWithWithPublishDateDesc() throws Exception {
        MediaForm form = form().publishDate(LONGAGO, LONGAGO.plusSeconds(5)).sortOrder(MediaSortOrder.desc(MediaSortField.publishDate)).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(5);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i).getResult().getLastPublished()).isBefore(result.getItems().get(i - 1).getResult().getLastPublished());
        }
    }

    @Test
    public void testFindWithWithDuration() throws Exception {
        MediaForm form = form().duration(Duration.ZERO, Duration.ofMillis(200)).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(2);
    }


    @Test
    @Ignore("Dropped support for finding by urn")
    public void testFindWithWithUrnMediaId() throws Exception {
        MediaForm form = form().mediaIds(programBuilder.build().getUrn()).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithWithBroadcasterWithVariedCaseMiss() throws Exception {

        MediaForm form = form().broadcasters("TVDrenthe").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(0);
    }

    @Test
    public void testFindWithWithBroadcasterWithVariedCaseHit() throws Exception {
        MediaForm form = form().broadcasters("TVDRENTHE").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithLocationsExtensionWithVariedCase() throws Exception {
        MediaForm form = form().locations("mP3").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithTags() throws Exception {
        MediaForm form = form().tags("Tag 2").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(3);
    }

    @Test
    public void testFindWithTagsIgnoreCase() throws Exception {
        MediaForm form = form().tags(Match.SHOULD, new ExtendedTextMatcher("OnderKast", ExtendedMatchType.TEXT, false)).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(2);
    }


    @Test
    public void testFindWithExcludeMediaIds() throws Exception {
        MediaForm form = form().mediaIds(Match.NOT, "MID-1", "MID-2").build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        System.out.println("LIST" + result.getItems());
        assertThat(result.getSize()).isEqualTo(indexedObjectCount - 2 /* excluded */);
    }


    @Test
    public void testFindWithExcludeTypes() throws Exception {
        MediaForm form = form().types(Match.NOT, MediaType.BROADCAST).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        // so, just the groups
        assertThat(result.getSize()).isEqualTo(indexedGroupCount);
    }


    @Test
    public void testFindWithAVType() throws Exception {
        MediaForm form = form().avTypes(Match.MUST, AVType.AUDIO).build();
        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        assertThat(result.getSize()).isEqualTo(4);
        assertThat(result.getItems().get(0).getResult().getAVType()).isEqualTo(AVType.AUDIO);
    }

    @Test
    public void testFindWithBroadcasterProfile() throws Exception {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("OMROEP1"))
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(6);
    }

    @Test
    public void testFindWithAFFileExtensionProfile() throws Exception {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new AVFileExtensionConstraint("MP3"))
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithHasPredictionsProfile() throws Exception {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new HasPredictionConstraint())
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    public void testFindWithHighlights() throws Exception {
        MediaForm form = form().text("title").highlight(true).build();

        MediaSearchResult result = target.find(null, form, 1L, 5);

        assertThat(result.getTotal()).isEqualTo(indexedObjectCount - 1); // One object has a different title
        SearchResultItem<? extends MediaObject> firstResult = result.getItems().get(0);
        assertThat(firstResult.getHighlights()).hasSize(1);
//        Assertions.assertThat(firstResult.getHighlights().get(0).getBody()).containsExactly("Main <em class=\"hlt1\">title</em> MIS', 'Short <em class=\"hlt1\">title</em>', 'Episode <em class=\"hlt1\">title</em> MIS");
    }

    @Test
    public void testFindMembersOnMid() throws Exception {
        assertRelatedMediaResult(target.findMembers(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindMembersOnUrn() throws Exception {
        assertRelatedMediaResult(target.findMembers(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindEpisodes() throws Exception {
        assertRelatedMediaResult(target.findEpisodes(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindDescendants() throws Exception {
        assertRelatedMediaResult(target.findDescendants(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindDescendantsOrderedByMember() throws Exception {
        MediaForm form = MediaFormBuilder.form()
            .sortOrder(MediaSortOrder.asc(MediaSortField.member)).build();
        MediaSearchResult result =
            target.findDescendants(group_ordered, null, form, 0L, 10);
        List<MediaObject> resultList = result.asList();

        // program2, program3, sub_group, sub_program1, sub_program2, program1
        // or
        // program2, program3, sub_group, program1, sub_program1, sub_program2

        assertThat(resultList).contains(program2, atIndex(0));
        assertThat(resultList).contains(program3, atIndex(1));
        assertThat(resultList).contains(sub_group, atIndex(2));
        assertThat(resultList).contains(program1, atIndex(3));

        // TODO: doubtfull
        assertThat(resultList).contains(sub_program1, atIndex(4));
        assertThat(resultList).contains(sub_program2, atIndex(5));
    }

    @Test
    public void testFindDescendantsOrderedSortDate() throws Exception {
        MediaForm form = MediaFormBuilder.form().sortOrder(MediaSortOrder.asc(MediaSortField.sortDate)).build();
        MediaSearchResult result =
            target.findDescendants(group_ordered, null, form, 0L, 10);
        List<MediaObject> resultList = result.asList();

        assertThat(resultList.get(0)).isEqualTo(program3);
        assertThat(resultList.get(1)).isEqualTo(program2);
        assertThat(resultList.get(2)).isEqualTo(sub_program2);
        assertThat(resultList.get(3)).isEqualTo(program1);
        assertThat(resultList.get(4)).isEqualTo(sub_program1);
        assertThat(resultList.get(5)).isEqualTo(sub_group);
    }

    @Test
    public void testWithRelations() throws IOException {
        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        MediaSearchResult result = target.find(null, MediaFormBuilder.form().relationText(director, "Stanley Kubrick").build(), 0, null);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.iterator().next().getResult().getMainTitle()).isEqualTo("About Kubrick");
    }

    @Test
    public void testWithRelationsIgnoreCase() throws IOException {
        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        ExtendedTextMatcher kubrick = new ExtendedTextMatcher("StanLey KubRick", false);
        MediaSearchResult result = target.find(null, MediaFormBuilder.form().relationText(director, kubrick).build(), 0, null);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.iterator().next().getResult().getMainTitle()).isEqualTo("About Kubrick");
    }


    @Test
    public void testListDescendants() throws IOException {
        MediaResult result = target.listDescendants(group_ordered, null,  Order.ASC, 0L, 100);

        List<? extends MediaObject> resultList = result.getItems();
        assertThat(resultList).hasSize(6);
        // program3, program2, sub_program2, program1, sub_program1/sub_group
        assertThat(resultList.get(0)).isEqualTo(program3);
        assertThat(resultList.get(1)).isEqualTo(program2);
        assertThat(resultList.get(2)).isEqualTo(sub_program2);
        assertThat(resultList.get(3)).isEqualTo(program1);
        assertThat(resultList.get(4)).isEqualTo(sub_program1);
        assertThat(resultList.get(5)).isEqualTo(sub_group);
    }

    @Test
    public void testListDescendantsWithProfile() throws IOException {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );
        MediaResult result = target.listDescendants(group_ordered, omroepProfile, Order.ASC, 0L, 100);

        List<? extends MediaObject> resultList = result.getItems();
        assertThat(resultList).hasSize(5);
        for (MediaObject o : resultList) {
            assertThat(o.getBroadcasters()).contains(new Broadcaster("BNN"));
        }
        // program3, program2, sub_program2, program1, sub_program1/sub_group
        assertThat(resultList.get(0)).isEqualTo(program3);
        assertThat(resultList.get(1)).isEqualTo(program2);
        assertThat(resultList.get(2)).isEqualTo(sub_program2);
        assertThat(resultList.get(3)).isEqualTo(program1);
        assertThat(resultList.get(4)).isEqualTo(sub_program1);

    }

    @Test
    public void testFindWithChannel() throws IOException {
        MediaSearchResult result = target.find(null, MediaFormBuilder.form()
            .scheduleEvents(ScheduleEventSearch.builder().channel(Channel.NED1).build())
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate))
            .build(), 0L, 100);

        assertThat(result).hasSize(3);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID-0");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID-4");
        assertThat(result.getItems().get(2).getResult().getMid()).isEqualTo("MID-8");

    }

    @Test
    public void testFindWithChannels() throws IOException {
        MediaSearchResult result = target.find(null, MediaFormBuilder.form()
            .scheduleEvents(
                ScheduleEventSearch.builder().channel(Channel.NED1).match(Match.SHOULD).build(),
                ScheduleEventSearch.builder().channel(Channel.NED2).match(Match.SHOULD).build()
            )
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate))
            .build(), 0L, 100);

        assertThat(result).hasSize(6);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID-0");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID-1");
        assertThat(result.getItems().get(2).getResult().getMid()).isEqualTo("MID-4");
        assertThat(result.getItems().get(3).getResult().getMid()).isEqualTo("MID-5");

    }


    private static <T extends MediaObject> T index(T object) throws IOException, ExecutionException, InterruptedException {
        AbstractESRepositoryITest.client.index(new IndexRequest(indexName, getTypeName(object), object.getMid()).source(Jackson2Mapper.INSTANCE.writeValueAsBytes(object))).get();
        indexed.add(object);
        assertThat(object.getLastPublishedInstant()).isNotNull();
        indexed.sort((o1, o2) -> (int) (o1.getLastPublished().getTime() - o2.getLastPublished().getTime()));
        if (Workflow.REVOKES.contains(object.getWorkflow())) {
            deletedObjectCount++;
            if (object instanceof Program) {
                deletedProgramCount++;
            }
            if (object instanceof Group) {
                deletedGroupCount++;
            }
            log.info("{} Indexed deleted {} for {}", deletedObjectCount, object, object.getLastPublishedInstant());
        } else {
            if (! mids.add(object.getMid())) {
                throw new IllegalStateException("Object " + object + " was indexed already?");
            }
            indexedObjectCount++;
            if (object instanceof Program) {
                indexedProgramCount++;
            }
            if (object instanceof Group) {
                indexedGroupCount++;
            }
            log.info("{} Indexed {} for {}", mids.size(), object, object.getLastPublishedInstant());
        }
        //log.info(Jackson2Mapper.getPrettyInstance().writeValueAsString(object));


        return object;
    }


    @SuppressWarnings("unchecked")
    private <T> void assertRelatedMediaResult(SearchResult<T> result, Program expectedProgram) {
        assertThat(result.getItems()).hasSize(1);
        SearchResultItem<? extends T> actual = result.getItems().get(0);
        assertThat(actual.getResult()).isNotNull();
        assertThat(actual.getResult()).isEqualTo(expectedProgram);
    }
}
