package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.domain.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.PredicateTestResult;
import nl.vpro.domain.constraint.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.broadcaster.BroadcasterServiceLocator;
import nl.vpro.media.domain.es.Common;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.FilteringIterator;

import static nl.vpro.domain.api.FacetResults.toSimpleMap;
import static nl.vpro.domain.api.media.MediaFormBuilder.form;
import static nl.vpro.domain.media.AgeRating.*;
import static nl.vpro.domain.media.support.Workflow.PUBLISHED_AS_DELETED;
import static nl.vpro.elasticsearch.ElasticSearchIteratorInterface.getScrollIds;
import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.Mockito.mock;

/**
 * These are integration tests where the index is build in the @BeforeClass.
 * Test which don't need that are placed in {@link ESMediaRepositoryPart1ITest}
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */

@Log4j2
@TestMethodOrder(MethodOrderer.MethodName.class)
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

    private  static ESMediaRepository target;


    private static Group group;
    private static Group group_ordered;

    private static Program program1;
    private static Program program2;
    private static Program program3;

    private static Group sub_group;
    private static Program sub_program1;
    private static Program sub_program2;


    private static final String[] testTags = {"Onderkast", "Bovenkast", "Geen kast", "Hoge kast", "Lage kast"};

    static List<MediaObject> indexed = new ArrayList<>();

    static int indexedObjectCount = 0;
    static int indexedProgramCount = 0;
    static int indexedGroupCount = 0;

    static Set<String> mids = new TreeSet<>();
    static int deletedObjectCount = 0;
    static int deletedProgramCount = 0;
    static int deletedGroupCount = 0;


    @BeforeEach
    public void init() {

        // during debugging you could set this to false, which would simplify queries
        target.setScore(true);
    }

    @AfterEach
    public void checkScrollIds() {
        assertThat(getScrollIds()).isEmpty();
    }


    /**
     * Builds a test database
     * This method is also tests that ES is able to index all the mediaObject
     * with all their attributes
     */
    @Override
    protected void firstRun() throws InterruptedException, ExecutionException, IOException {
        target = new ESMediaRepository(staticClientFactory, "tags", new MediaScoreManagerImpl());
        createIndicesIfNecessary();

        BroadcasterServiceLocator.setInstance(mock(BroadcasterService.class));

        target.setIndexName(indexHelpers.get(APIMEDIA).getIndexName());

        group = index(groupBuilder.published());
        group_ordered = index(MediaTestDataBuilder.group().constrained().published(NOW).type(GroupType.SERIES).withMid());
        // 2 groups
        program1 = index(programBuilder.copy()
            .publishStart(LocalDateTime.of(2017, 1, 30, 0, 0)) // sortDate is relevant for listDescendants
            .memberOf(group, 1)
            .memberOf(group_ordered, 7).episodeOf(group, 3));
        program2 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 29, 0, 0))
            .published(NOW).withMid()
            .memberOf(group_ordered, 2));
        program3 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 28, 0, 0))
            .published(NOW)
            .withMid()
            .memberOf(group_ordered, 3));
        sub_group = index(MediaTestDataBuilder.group().published()
            .mid("sub_group")
            .memberOf(group_ordered, 4)
            .published(NOW)
            .creationDate(NOW)
            );
        sub_program1 = index(programBuilder.copy()
            .publishStart(LocalDateTime.of(2017, 1, 30, 1, 0)) // sortDate is relevant for listDescendants
            .mid("sub_program_1")
            .published(NOW)
            .creationDate(NOW)
            .memberOf(sub_group, 1)
            );
        sub_program2 = index(MediaTestDataBuilder.program().constrained()
            .publishStart(LocalDateTime.of(2017, 1, 29, 2, 0))
            .mid("sub_program_2")
            .published(NOW)
            .creationDate(NOW)
            .memberOf(sub_group, 2));

        // order of descendant of group_ordered by sortDate should be
        // program3, program2, sub_program2, program1, sub_program1/sub_group
        // by member logical would be
        // program2, program3, sub_group, sub_program1, sub_program2, program1
        // or
        // program2, program3, sub_group, program1, sub_program1, sub_program2
        //3 + 2 programs (broadcasts), 1 sub group


        index(MediaTestDataBuilder.group().constrained().type(GroupType.COLLECTION).mid("VPGROUP_D1").lastPublished(NOW).workflow(Workflow.DELETED).title("Deleted Group"));
        // 1 deleted group
        index(MediaTestDataBuilder.program().constrained().type(ProgramType.CLIP).mid("VPPROGRAM_D").lastPublished(NOW).workflow(Workflow.REVOKED).title("Deleted Program"));
        index(MediaTestDataBuilder.program().constrained().type(ProgramType.CLIP).mid("VPPROGRAM_D1").lastPublished(NOW).workflow(Workflow.MERGED).title("Deleted Merged Program").mergedTo(program1));
        // 2 deleted programs

        AgeRating[] ORIGINAL = {_6, _9, _12, _16, ALL};
        for (int i = 0; i < 10; i++) {
            Group g = index(MediaTestDataBuilder.group()
                .constrained()
                .workflow(Workflow.PUBLISHED)
                .broadcasters("OMROEP" + (i % 3))
                .creationDate(LONGAGO.plusSeconds(i))
                .lastPublished(LONGAGO.plusSeconds(i * 2))
                .withGenres()
                .mid("MID_G_" + i)
                );
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
                .ageRating(ORIGINAL[i % ORIGINAL.length])
                .predictions(Platform.INTERNETVOD)
                .mid("MID-" + i)
                );
        }
        // 10 groups, and 10 programs (broadcasts)

        // totals now 30 objects, 13 groups, 15 broadcasts (from which 3 deleted)


        // index some variations
        index(programBuilder.copy().mid("MID_HIGH_SCORE").mainTitle("This should give us a high score"));
        // 29 objects, 14 programs


        index(groupBuilder.copy().mid("MID_SCORING_ON_DESCRIPTION")
                .mainDescription("While scoring a hit on a description field is likely to receive a much lower score then a hit on a title."));

        // 30, 13 groups

        Location wm = new Location("http://somedomain.com/path/to/file", OwnerType.BROADCASTER);
        wm.setAvFileFormat(AVFileFormat.WM);

        index(programBuilder.copy().mid("MID_WITH_LOCATION").locations(wm));

        index(programBuilder.copy().mid("MID_DRENTHE").broadcasters(new Broadcaster("TVDRENTHE", "TVDrenthe")));

        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        index(programBuilder.copy().mid("MID_WITH_RELATIONS").mainTitle("About Kubrick").relations(new Relation(director, "", "Stanley Kubrick")));

        // 33 (3 deleted)
        refresh();

        assertThat(indexedObjectCount).isEqualTo(indexedGroupCount + indexedProgramCount);
        assertThat(deletedObjectCount).isEqualTo(deletedGroupCount + deletedProgramCount);
    }


    @Test
    public void testGroupBy() throws IOException {

        SearchSourceBuilder source = new SearchSourceBuilder();
        source.aggregation(AggregationBuilders.terms("workflows")
            .field("workflow")
            .order(BucketOrder.key(true)));
        source.aggregation(AggregationBuilders.terms("objectTypes")
            .field("objectType")
            .order(BucketOrder.key(true)));
        source.aggregation(AggregationBuilders.terms("types")
            .field("type")
            .order(BucketOrder.key(true)));
        source.size(0);

        SearchRequest request = new SearchRequest(indexHelpers.get(APIMEDIA).getIndexName());
        request.source(source);
        SearchResponse response = highLevelClient().search(request, RequestOptions.DEFAULT);

        {
            Terms a = response.getAggregations().get("workflows");
            String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
            assertThat(result).isEqualTo("DELETED:1,MERGED:1,PUBLISHED:33,REVOKED:1");
        }
        {
            Terms a = response.getAggregations().get("objectTypes");
            String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
            assertThat(result).isEqualTo("group:15,program:21");
        }
        {
            Terms a = response.getAggregations().get("types");
            String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
            assertThat(result).isEqualTo("BROADCAST:19,CLIP:2,COLLECTION:1,PLAYLIST:10,SERIES:3");
        }
    }

    @Test
    public void testLoad() {
        Program in = programBuilder.build();
        MediaObject result = target.load(in.getMid());

        assertThat(result).isEqualTo(in);
    }

    @Test
    public void testLoadAll() {
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
        log.info("All mids: {}", results.stream().map(MediaObject::getMid).collect(Collectors.toList()));
    }

    @Test
    public void testListWithOffset() {
        MediaResult results = target.list(Order.ASC, 10L, 1000);
        assertThat(results).hasSize(indexedObjectCount - 10);

    }

    @Test
    public void testMediaChanges() {
        Iterator<MediaChange> changes = target.changes(LONGAGO.minus(1, ChronoUnit.SECONDS), null, null, null, Order.ASC, Integer.MAX_VALUE, null, null, null);
        List<MediaChange> list = new ArrayList<>();
        changes.forEachRemaining(list::add);
        assertThat(list.stream().filter(MediaChange::isDeleted).collect(Collectors.toList())).hasSize(3);
        assertThat(list).hasSize(indexedObjectCount + deletedObjectCount);
    }


    @Test
    public void testMediaChangesExcludeDeletes() throws Exception {
        try (CloseableIterator<MediaChange> changes = target.changes(LONGAGO.minus(1, ChronoUnit.SECONDS), null, null, null, Order.ASC, Integer.MAX_VALUE, Deletes.EXCLUDE, null, null)) {
            List<MediaChange> list = new ArrayList<>();
            changes.forEachRemaining(list::add);
            assertThat(list.stream().filter(Change::isNotSkipped).collect(Collectors.toList())).hasSize(indexedObjectCount);
            assertThat(list.stream().filter(Change::isSkipped).collect(Collectors.toList())).hasSize(deletedObjectCount);
            assertThat(list).hasSize(indexedObjectCount + deletedObjectCount);
        }
    }

    @Test
    public void testMediaChangesSince() throws Exception {
        try (CloseableIterator<MediaChange> changes = target.changes(NOW.minus(1, ChronoUnit.SECONDS), null, null, null, Order.DESC, Integer.MAX_VALUE, null, null, null)) {
            List<MediaChange> list = new ArrayList<>();
            changes.forEachRemaining(list::add);
            assertThat(list).hasSize(indexedObjectCount - 17); // 17 objects created around EPOCH
            assertThat(list.stream().filter(MediaChange::isDeleted).collect(Collectors.toList())).hasSize(3);
            Instant prev = Instant.MIN;

            for (MediaChange c : list) {
                assertThat(c.getPublishDate().isBefore(prev)).isFalse();
                prev = c.getPublishDate();
            }
        }
    }

    @Test
    public void testMediaChangesSinceWithMax() throws Exception {
        Instant prev = NOW.minus(1, ChronoUnit.SECONDS);
        try (CloseableIterator<MediaChange> changes = target.changes(prev, null, null, null, Order.DESC, 5, null, null, null)) {
            List<MediaChange> list = new ArrayList<>();
            changes.forEachRemaining(list::add);
            assertThat(list).hasSize(5);

            for (MediaChange c : list) {
                assertThat(c.getPublishDate().isBefore(prev)).isFalse();
                prev = c.getPublishDate();
                log.info("{}", c);
            }
        }
    }

    @Test
    public void testMediaChangesWithMax() throws Exception {
        try (CloseableIterator<MediaChange> changes = target.changes(Instant.EPOCH, "MID_DRENTHE", null, null, Order.DESC, 10, null, null, null)) {
            List<MediaChange> list = new ArrayList<>();
            changes.forEachRemaining(list::add);
            assertThat(list).hasSize(10);
        }
    }

    @Test
    public void testIterate() throws Exception {
        target.iterateBatchSize = 10;
        try (CloseableIterator<MediaObject> results = target.iterate(null, null, 0L, 1000, FilteringIterator.noKeepAlive())) {
            assertThat(results).toIterable().hasSize(indexedObjectCount);
        }
    }

    @Test
    public void testIterateWithOffset() throws Exception {
        target.iterateBatchSize = 10;
        try (CloseableIterator<MediaObject> results = target.iterate(null, null, 10L, 1000, FilteringIterator.noKeepAlive())) {
            assertThat(results).toIterable().hasSize(indexedObjectCount - 10);
        }
    }


    @Test
    public void testLoadNotFound() {
        MediaObject object = target.findByMid("bestaatniet");

        assertThat(object).isNull();
    }

    @Test
    public void testFindAll() {
        SearchResult<MediaObject> result = target.find(null, null, 0L, 100);
        assertThat(result.asList().stream().map(MediaObject::getMid).sorted()).containsExactlyElementsOf(mids);

        assertThat(result.getTotal()).isEqualTo(indexedObjectCount);
        assertThat(result.getTotalQualifier()).isEqualTo(Result.TotalQualifier.EQUAL_TO);


    }

    @Test
    public void  testFind() {
        SearchResult<MediaObject> result = target.find(null, null, 2L, 5);
        assertThat(result.getTotal()).isEqualTo(indexedObjectCount);
        assertThat(result.getOffset()).isEqualTo(2);
        assertThat(result.getMax()).isEqualTo(5);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getItems()).hasSize(5);
    }


    @Test
    public void testFindOnProfileWithTextScore() {
        MediaForm form = form().text("Text with Score words").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getItems().get(0).getResult()).isEqualTo(programBuilder.build());
        assertThat(result.getItems().get(1).getResult()).isEqualTo(groupBuilder.build());
    }

    @Test
    public void testFindWithWithMidMediaId() {
        MediaForm form = form().mediaIds(programBuilder.build().getMid()).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithWithMidMediaIds() {
        MediaForm form = form().mediaIds("MID-1", "MID-2").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    public void testFindWithWithSortDate() {
        MediaForm form = form().asc(MediaSortField.sortDate).sortDate(NOW,  NOW.plus(Duration.ofHours(2))).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i).getResult().getSortInstant()).isAfterOrEqualTo(result.getItems().get(i - 1).getResult().getSortInstant());
        }
    }


    @Test
    public void testFindWithWithPublishDateAsc() {
        MediaForm form = form()
            .publishDate(LONGAGO, LONGAGO.plusSeconds(5))
            .sortOrder(MediaSortOrder.asc(MediaSortField.publishDate))
            .build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(5);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i)
                .getResult()
                .getLastPublishedInstant())
                .isAfter(result.getItems().get(i - 1).getResult()
                    .getLastPublishedInstant());
        }
    }


    @Test
    public void testFindWithWithPublishDateDesc() {
        MediaForm form = form()
            .publishDate(LONGAGO, LONGAGO.plusSeconds(5))
            .sortOrder(MediaSortOrder.desc(MediaSortField.publishDate))
            .build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(5);
        for (int i = 1; i < result.getItems().size(); i++) {
            assertThat(result.getItems().get(i).getResult().getLastPublishedInstant()).isBefore(result.getItems().get(i - 1).getResult().getLastPublishedInstant());
        }
    }

    @Test
    public void testFindWithWithDuration() {
        MediaForm form = form().duration(Duration.ZERO, Duration.ofMillis(200)).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);
    }


    @Test
    @Disabled("Dropped support for finding by urn")
    public void testFindWithWithUrnMediaId() {
        MediaForm form = form().mediaIds(programBuilder.build().getUrn()).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithWithBroadcasterWithVariedCaseMiss() {

        MediaForm form = form().broadcasters("TVDrenthe").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(0);
    }

    @Test
    public void testFindWithWithBroadcasterWithVariedCaseHit() {
        MediaForm form = form().broadcasters("TVDRENTHE").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithLocationsExtensionWithVariedCase() {
        MediaForm form = form().locations("mP3").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithTags() {
        MediaForm form = form().tags("Tag 2").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(3);
    }

    @Test
    public void testFindWithTagsIgnoreCase() {
        MediaForm form = form().tags(Match.SHOULD, new ExtendedTextMatcher("OnderKast", StandardMatchType.TEXT, false)).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);
    }


    @Test
    public void testFindWithExcludeMediaIds() {
        MediaForm form = form().mediaIds(Match.NOT, "MID-1", "MID-2").build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        System.out.println("LIST" + result.getItems());
        assertThat(result.getSize()).isEqualTo(indexedObjectCount - 2 /* excluded */);
    }


    @Test
    public void testFindWithExcludeTypes() {
        MediaForm form = form().types(Match.NOT, MediaType.BROADCAST).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        // so, just the groups
        assertThat(result.getSize()).isEqualTo(indexedGroupCount);
    }


    @Test
    public void testFindWithAVType() {
        MediaForm form = form().avTypes(Match.MUST, AVType.AUDIO).build();
        SearchResult<MediaObject> result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(4);
        assertThat(result.getItems().get(0).getResult().getAVType()).isEqualTo(AVType.AUDIO);
    }

    @Test
    public void testFindWithBroadcasterProfile() {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("OMROEP1"))
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(6);
    }

    @Test
    public void testFindWithAFFileExtensionProfile() {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new AVFileExtensionConstraint("MP3"))
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithHasPredictionsProfile() {
        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new HasPredictionConstraint())
        );
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    public void testFindWithHighlights() {
        MediaForm form = form().text("title").highlight(true).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getTotal()).isEqualTo(indexedObjectCount - 1); // One object has a different title
        SearchResultItem<? extends MediaObject> firstResult = result.getItems().get(0);
        assertThat(firstResult.getHighlights()).hasSize(2);
//        Assertions.assertThat(firstResult.getHighlights().get(0).getBody()).containsExactly("Main <em class=\"hlt1\">title</em> MIS', 'Short <em class=\"hlt1\">title</em>', 'Episode <em class=\"hlt1\">title</em> MIS");
    }

    @Test
    public void testFindMembersOnMid() {
        assertRelatedMediaResult(target.findMembers(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindMembersOnUrn() {
        assertRelatedMediaResult(target.findMembers(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindEpisodes() {
        assertRelatedMediaResult(target.findEpisodes(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindDescendants() {
        assertRelatedMediaResult(target.findDescendants(group, null, null, 0L, 10), program1);
    }

    @Test
    public void testFindDescendantsOrderedByMember() {
        MediaForm form = MediaFormBuilder.form()
            .sortOrder(MediaSortOrder.asc(MediaSortField.member)).build();
        MediaSearchResult result =
            target.findDescendants(group_ordered, null, form, 0L, 10);
        List<MediaObject> resultList = result.asList();

        // program2, program3, sub_group, sub_program1, sub_program2, program1
        // or
        // program2, program3, sub_group, program1, sub_program1, sub_program2

        assertThat(resultList).hasSize(6);
        assertThat(resultList).contains(program2, atIndex(0));
        assertThat(resultList).contains(program3, atIndex(1));
        assertThat(resultList).contains(sub_group, atIndex(2));
        assertThat(resultList).contains(program1, atIndex(3));

        // We can't really know the order because these are not direct members of group_ordered
        // We here  say that they should be ordered by descending sort date
        // Should we indeed garantee that?
        assertThat(resultList).contains(sub_program1, atIndex(4));
        assertThat(resultList).contains(sub_program2, atIndex(5));
    }

    @Test
    public void testFindDescendantsOrderedSortDate() {
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
    public void testWithRelations() {

        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        MediaSearchResult result = getAndTestResult(MediaFormBuilder.form().relationText(director, "Stanley Kubrick"));
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.iterator().next().getResult().getMainTitle()).isEqualTo("About Kubrick");
    }

    @Test
    public void testWithRelationsIgnoreCase() {
        RelationDefinition director = RelationDefinition.of("director", "VPRO");

        ExtendedTextMatcher kubrick = new ExtendedTextMatcher("StanLey KubRick", false);
        MediaSearchResult result = getAndTestResult(MediaFormBuilder.form().relationText(director, kubrick));
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.iterator().next().getResult().getMainTitle()).isEqualTo("About Kubrick");
    }


    @Test
    public void testListDescendants() {
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
    public void testListDescendantsWithProfile() {
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
    public void testFindWithChannel() {
        MediaSearchResult result = getAndTestResult(MediaFormBuilder.form()
            .scheduleEvents(ScheduleEventSearch.builder().channel(Channel.NED1).build())
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate))
        );

        assertThat(result).hasSize(3);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID-0");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID-4");
        assertThat(result.getItems().get(2).getResult().getMid()).isEqualTo("MID-8");

    }

    @Test
    public void testFindWithChannels() {
        MediaSearchResult result = getAndTestResult(MediaFormBuilder.form()
            .scheduleEvents(
                ScheduleEventSearch.builder().channel(Channel.NED1).match(Match.SHOULD).build(),
                ScheduleEventSearch.builder().channel(Channel.NED2).match(Match.SHOULD).build()
            )
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate)));

        assertThat(result).hasSize(6);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID-0");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID-1");
        assertThat(result.getItems().get(2).getResult().getMid()).isEqualTo("MID-4");
        assertThat(result.getItems().get(3).getResult().getMid()).isEqualTo("MID-5");

    }

    @Test
    public void testWithMultipleFacetsAndFiltersAndProfile() {
        target.setScore(false);

        LocalDateTime since = LocalDateTime.of(2010, 1, 1, 12, 0);
        // for reference: These are all mids:
        // [MID-0, MID-1, MID-2, MID-3, MID-4, MID-5, MID-6, MID-7, MID-8, MID-9, MID_DRENTHE, MID_G_0, MID_G_1, MID_G_2, MID_G_3, MID_G_4, MID_G_5, MID_G_6, MID_G_7, MID_G_8, MID_G_9, MID_HIGH_SCORE, MID_SCORING_ON_DESCRIPTION, MID_WITH_LOCATION, MID_WITH_RELATIONS, POMS_S_12345, VPROWON_12346, VPROWON_12349, VPROWON_12351, VPROWON_12353, sub_group, sub_program_1, sub_program_2]
        MediaForm form = MediaFormBuilder.form()
            /*
            According to the documentation:
            By default,the count of a facet result is over the entire search result (i.e.not limited by the 'max' property).
            It is possible to limit the facet results global or per individual facet(from release version REL - 3.3).To
            limit the facet result a filter property must be added globally or on an individual facet.
           */
            .broadcasterFacet(MediaFacet.builder()
                // So, in this case we limit to only a few mids, so the total number in the facet results must be equal to that
                .filter(MediaSearch.builder()
                    .mediaIds(
                        TextMatcherList.must(
                            TextMatcher.must("MID-[024]", StandardMatchType.REGEX)) // only 3 objects match
                        // namely
                        // MID-0, BNN, AVRO,OMROEP0
                        // MID-2, BNN, AVRO,OMROEP2
                        // MID-4, BNN, AVRO,OMROEP1
                    )
                    .build())
                .build())
            .ageRatingFacet(MediaFacet.builder()
                // And this case is limited even further
                .filter(MediaSearch.builder()
                    .mediaIds(
                        TextMatcherList.must(
                            TextMatcher.must("MID-[04]", StandardMatchType.REGEX)) // only 2 objects match
                        // namely
                        // MID-0, _6
                        // MID-4, ALL
                    )
                    .build()
                )
                .build()
            )
            .facetFilter(MediaSearch.builder()
                .sortDates(DateRangeMatcherList
                    .builder()
                    .value(
                        DateRangeMatcher.builder()
                            .localBegin(since)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .build();

        ProfileDefinition<MediaObject> ageRatingProfile = new ProfileDefinition<>(
            new Filter(new Not(new AgeRatingConstraint(AgeRating._16)))
        );

        List<MediaObject> indexedInProfile = new ArrayList<>();
        for (MediaObject m : indexed) {
            PredicateTestResult predicateTestResult = ageRatingProfile.getPredicate().testWithReason(m);
            boolean applies = predicateTestResult.applies();
            if (m.getAgeRating() == AgeRating._16) {
                if (applies) {
                    fail("Unexpected");
                }
                log.info("Skipping {} because agerating", m);
                continue;
            }
            if (! applies) {
                fail("Unexpected");
            }
            indexedInProfile.add(m);
        }
        Map<String, Long> broadcasterFacet;
        Map<String, Long> ageRatingFacet;

        {
            Map<String, AtomicLong> broadcasterFacetAtomic = new HashMap<>();
            Map<String, AtomicLong> ageRatingFacetAtomic = new HashMap<>();
            for (MediaObject m : indexedInProfile) {
                if (m.getSortInstant() == null || m.getSortInstant().isBefore(since.atZone(Schedule.ZONE_ID).toInstant())) {
                    continue;
                }
                if (Pattern.compile("MID-[024]").matcher(m.getMid()).matches()) {
                    for (Broadcaster b : m.getBroadcasters()) {
                        broadcasterFacetAtomic.computeIfAbsent(b.getId(), (br) -> new AtomicLong(0)).incrementAndGet();
                    }
                }
                if (Pattern.compile("MID-[04]").matcher(m.getMid()).matches()) {
                    ageRatingFacetAtomic.computeIfAbsent(m.getAgeRating().getXmlValue(), (br) -> new AtomicLong(0)).incrementAndGet();
                }
            }
            broadcasterFacet = sort(broadcasterFacetAtomic);
            ageRatingFacet =  sort(ageRatingFacetAtomic);
        }

        log.info("{}", broadcasterFacet);

        MediaSearchResult result = target.find(ageRatingProfile, form, 0L, 100);

        List<TermFacetResultItem> broadcasters = result
            .getFacets()
            .getBroadcasters();
        assertThat(broadcasters).isNotNull();
        Map<String, Long> simple = toSimpleMap(broadcasters);

        assertThat(simple).isEqualTo(broadcasterFacet);

        assertThat(broadcasters).hasSize(5);
        long totalBroadcasterCount = broadcasters.stream().mapToLong(FacetResultItem::getCount).sum();
        //// 3 with BNN + 3 with AVRO + 1 with OMROEP0, 1 with OMROEP2, 0 with OMROEP1, total 9.
        assertThat(totalBroadcasterCount).isEqualTo(9);


        List<TermFacetResultItem> ageRatings = result.getFacets().getAgeRatings();
        assertThat(ageRatings).isNotNull();
        long totalAgeRatingCount = ageRatings.stream().mapToLong(FacetResultItem::getCount).sum();

        assertThat(toSimpleMap(ageRatings)).isEqualTo(ageRatingFacet);
        assertThat(totalAgeRatingCount).isEqualTo(2);
        assertThat(ageRatings).hasSize(2);


        assertThat(totalBroadcasterCount).isNotEqualTo(totalAgeRatingCount);

        assertThat(ageRatings.get(0).getId()).isEqualTo("6");
        assertThat(ageRatings.get(0).getCount()).isEqualTo(1);
        assertThat(ageRatings.get(1).getId()).isEqualTo("ALL");
        assertThat(ageRatings.get(1).getCount()).isEqualTo(1);

        log.info("{}", result);

    }

    @Test
    public void withMaxZero() {
        MediaResult result = target.listMembers(target.load("POMS_S_12345"), null, Order.ASC, 0L, 10);

        log.info("{}", result.getTotal());


    }


    private static <T extends MediaObject> T index(MediaBuilder<?, T> builder) throws IOException {
        if (! PUBLISHED_AS_DELETED.contains(builder.getWorkflow())) {
             builder.workflow(Workflow.PUBLISHED);
        }

        T object = builder.build();

        Consumer<ObjectNode> addPublishdate = addPublishDate(object.getLastPublishedInstant());

        indexHelpers.get(APIMEDIA).index(
            object.getMid(),
            map(object, addPublishdate));

        for (MemberRef r : object.getMemberOf()) {
            StandaloneMemberRef ref = StandaloneMemberRef.builder().memberRef(r).build();
            indexHelpers.get(APIMEDIA_REFS).indexWithRouting(
                ref.getId().toString(),
                map(ref, addPublishdate),
                ref.getMidRef());
        }
        indexed.add(object);
        assertThat(object.getLastPublishedInstant()).isNotNull();
        indexed.sort((o1, o2) -> (int) (o1.getLastPublishedInstant().toEpochMilli() - o2.getLastPublishedInstant().toEpochMilli()));
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

    static byte[] map(Object obj, Consumer<ObjectNode> consumer) throws JsonProcessingException {
        ObjectNode jsonNode = Jackson2Mapper.getPublisherInstance().valueToTree(obj);
        consumer.accept(jsonNode);
        return Jackson2Mapper.getPublisherInstance().writeValueAsBytes(jsonNode);

    }
    static Consumer<ObjectNode> addPublishDate(Instant now){
        return (jsonNode) -> jsonNode.put(Common.ES_PUBLISH_DATE, now.toEpochMilli());
    }


    private <T> void assertRelatedMediaResult(SearchResult<T> result, Program expectedProgram) {
        assertThat(result.getItems()).hasSize(1);
        SearchResultItem<? extends T> actual = result.getItems().get(0);
        assertThat(actual.getResult()).isNotNull();
        assertThat(actual.getResult()).isEqualTo(expectedProgram);
    }


    protected Map<String, Long> sort(Map<String, AtomicLong> fromResult) {
       return fromResult
            .entrySet()
            .stream()
            .map((e) -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().get()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                LinkedHashMap::new));
    }

    protected MediaSearchResult getAndTestResult(MediaForm form) {
        return getAndTestResult(target, form);
    }
    protected MediaSearchResult getAndTestResult(MediaFormBuilder form) {
        return getAndTestResult(form.build());
    }
}
