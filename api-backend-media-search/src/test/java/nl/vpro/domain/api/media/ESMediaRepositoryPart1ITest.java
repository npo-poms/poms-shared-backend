package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXB;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import nl.vpro.api.Settings;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.MediaESType;

import static nl.vpro.domain.api.media.MediaFormBuilder.form;
import static nl.vpro.domain.media.AgeRating.*;
import static nl.vpro.domain.media.ContentRating.*;
import static nl.vpro.domain.media.MediaTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 *
 * See also {@link ESMediaRepositoryPart2ITest} This test sets up de index for
 * every test. Part2 creates a bunch of test data in @Setup. Choose what is more
 * convenient for your new tests.
 *
 * @author Roelof Jan Koekoek
 * @since 3.5
 */

@SuppressWarnings("ConstantConditions")
@ContextConfiguration(locations = "classpath:nl/vpro/domain/api/media/ESMediaRepositoryITest-context.xml")
@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ESMediaRepositoryPart1ITest extends AbstractMediaESRepositoryITest {

    @Autowired
    private ESMediaRepository target;

    @Autowired
    private Settings settings;


    @Override
    protected void firstRun() throws Exception {
        createIndexIfNecessary(ApiMediaIndex.NAME);
    }
    @Before
    public  void setup() throws Exception {
        target.setIndexName(indexName);
        clearIndex();
    }



    @Before
    public void before() {
        Mockito.reset(target.mediaRepository);
        settings.setRedirectsRepository("COUCHDB");
        Map<String, String> redirects = new HashMap<>();
        when(target.mediaRepository.redirects()).thenReturn(new RedirectList(null, null, redirects));
    }



    @Test
    public void testLoad() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("foo bar").mid("MID_FOR_LOAD").build());
        MediaObject result = target.load("MID_FOR_LOAD");
        assertThat(result.getMainTitle()).isEqualTo("foo bar");
        assertThat(result.getMid()).isEqualTo("MID_FOR_LOAD");

    }

    @Test
    public void testText() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("foo").build());
        index(program().mainTitle("bar").build());

        {
            SearchResult<MediaObject> result = target.find(null, form().text("foo").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().text("foa").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().text("FOO").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }
    }

    @Test
    public void testTextFuzzy() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("foo").build());
        index(program().mainTitle("foa").build());
        index(program().mainTitle("bar").build());

        {
            SearchResult<MediaObject> result = target.find(null, form().fuzzyText("foa").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            List<? extends SearchResultItem<? extends MediaObject>> items = result.getItems();
            assertThat(items.get(1).getScore()).isLessThanOrEqualTo(items.get(0).getScore());
            Assert.assertTrue(items.stream().anyMatch(item -> item.getResult().getMainTitle().equals("foo")));
            Assert.assertTrue(items.stream().anyMatch(item -> item.getResult().getMainTitle().equals("foa")));
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().text("FOO").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }
    }

    @Test
    public void testFindTagText() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").tags("foo", "bar").build());
        index(program().mainTitle("t2").tags("xxx", "yyy").build());

        {
            SearchResult<MediaObject> result = target.find(null, form().tags("foo").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

        {
            SearchResult<MediaObject> result = target.find(null, form().tags("FOO").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(Match.MUST, ExtendedTextMatcher.must("FOO", false)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

    }

    @Test
    public void testFindTagTextRegex() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").tags("foo", "bar").build());
        index(program().mainTitle("t2").tags("xxx", "yyy").build());

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("fo.*", ExtendedMatchType.REGEX)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("FO.*", ExtendedMatchType.REGEX)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("FO.*", ExtendedMatchType.REGEX, false)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

    }

    @Test
    public void testFindTagWildcard() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").tags("foobar", "xxxyyyy").build());
        index(program().mainTitle("t2").tags("xxx", "yyy").build());

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("fo*bar", ExtendedMatchType.WILDCARD)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("FO*BAR", ExtendedMatchType.WILDCARD)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("FO*BAR", ExtendedMatchType.WILDCARD, false)).build(), 0,
                    null);
            assertThat(result.getSize()).isEqualTo(1);
        }
    }

    @Test
    public void testFindWithHasImageProfile() throws Exception {
        index(program().withMid().build());

        final Program withImages = program().withMid().withImages().build();
        withImages.getImages().get(0).setId(2L);
        index(withImages);

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(new Filter(new HasImageConstraint()));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithHasLocationsProfile() throws Exception {
        index(program().withMid().build());

        final Program withLocations = program().withMid().withLocations().build();
        withLocations.getLocations().first().setId(2L);
        index(withLocations);

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(new Filter(new HasLocationConstraint()));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithFacetOrderings() throws Exception {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("A"), new Broadcaster("B"))
                .build());

        {
            MediaForm countAsc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.COUNT_ASC, null)).build();
            MediaSearchResult result = target.find(null, countAsc, 0, null);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
        }

        {
            MediaForm countDesc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.COUNT_DESC, null)).build();
            MediaSearchResult result = target.find(null, countDesc, 0, null);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("B");
        }

        {
            MediaForm valueAsc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.VALUE_ASC, null)).build();
            MediaSearchResult result = target.find(null, valueAsc, 0, null);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
        }

        {
            MediaForm valueDesc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.VALUE_DESC, null)).build();
            MediaSearchResult result = target.find(null, valueDesc, 0, null);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("B");
        }
    }

    @Test
    public void testFindWithFacetWithThreshold() throws Exception {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("B")).build());

        index(program().withMid().broadcasters(new Broadcaster("A")).build());

        MediaForm form = form().broadcasterFacet(new MediaFacet(2, FacetOrder.VALUE_ASC, null)).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getBroadcasters()).hasSize(1);
        assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
    }

    @Test
    public void testFindWithFacetWithMax() throws Exception {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("B")).build());

        MediaForm form = form().broadcasterFacet(new MediaFacet(null, FacetOrder.VALUE_ASC, 1)).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getBroadcasters()).hasSize(1);
        assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
    }

    @Test
    public void testFindWithAvTypeFacet() throws Exception {
        index(program().withMid().withAVType().build());

        MediaSearchResult result = target.find(null, form().avTypes(AVType.VIDEO).avTypeFacet().build(), 0, null);

        assertThat(result.getFacets().getAvTypes()).isNotEmpty();
        List<TermFacetResultItem> avTypes = result.getFacets().getAvTypes();
        for (TermFacetResultItem avType : avTypes) {
            if (avType.getId().equals(AVType.VIDEO.name())) {
                assertThat(avType.getCount()).isEqualTo(1L);
            } else {
                assertThat(avType.getCount()).isEqualTo(0);
            }
        }
    }

    @Test
    public void testFindWithTypeFacet() throws Exception {
        index(program().withMid().withType().build());

        MediaForm form = form().typeFacet().build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getTypes()).isNotEmpty();
        List<TermFacetResultItem> types = result.getFacets().getTypes();
        for (TermFacetResultItem type : types) {
            if (type.getId().equals(ProgramType.BROADCAST.name())) {
                assertThat(type.getCount()).isEqualTo(1);
            } else {
                assertThat(type.getCount()).isEqualTo(0);
            }
        }
    }

    @Test
    public void testFindWithSortDateFacet() throws Exception {
        index(program().withMid().withPublishStart().build());

        // both ranges
        MediaForm form = form().sortDateFacet(
            DateRangePreset.TODAY,
            DateRangePreset.THIS_WEEK,
            DateRangeFacetItem.builder()
                .begin(Instant.now().minus(Duration.ofHours(100)))
                .end(Instant.now())
                .name("100hours")
                .build()
        ).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getSortDates()).isNotEmpty();
    }


    @Test
    public void testFindWithSortDateFacetHistogram() throws Exception {
        index(program().withMid().withPublishStart().build());

        // both ranges
        MediaForm form = form().sortDateFacet(
            new DateRangeInterval("1 year")
        ).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getSortDates()).isNotEmpty();
    }

    @Test
    public void testFindWithDurationFacetHistogram() throws Exception {
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)).build());
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)).build());
        index(program().withMid().duration(Duration.of(3, ChronoUnit.HOURS)).build());

        DurationRangeInterval interval = new DurationRangeInterval("1 hour");
        MediaForm form = form().durationFacet(interval).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        for (SearchResultItem<? extends MediaObject> mo : result) {
            System.out.println(mo.getResult().getDuration());
        }

        assertThat(result.getFacets().getDurations()).isNotEmpty();
        assertThat(result.getFacets().getDurations()).hasSize(2);

        assertThat(result.getFacets().getDurations().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getDurations().get(1).getCount()).isEqualTo(1);



    }


    @Test
    public void testFindWithDurationFacet() throws Exception {
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)).build());
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)).build());
        index(program().withMid().duration(Duration.of(3, ChronoUnit.HOURS)).build());

        MediaForm form = form().durationFacet(
            DurationRangeFacetItem.builder()
                .name("less than 2 hours")
                .end(Duration.ofHours(2))
                .build(),
            DurationRangeFacetItem.builder()
                .name("more than 2 hours")
                .begin(Duration.ofHours(2))
                .build()
        ).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        for (SearchResultItem<? extends MediaObject> mo : result) {
            System.out.println(mo.getResult().getDuration());
        }

        assertThat(result.getFacets().getDurations()).isNotEmpty();
        assertThat(result.getFacets().getDurations()).hasSize(2);

        assertThat(result.getFacets().getDurations().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getDurations().get(1).getCount()).isEqualTo(1);
        assertThat(result.getFacets().getDurations().get(0).getValue()).isEqualTo("less than 2 hours");
        assertThat(result.getFacets().getDurations().get(1).getValue()).isEqualTo("more than 2 hours");


    }


    @Test
    public void testFindWithGenreFacet() throws Exception {
        index(program().withMid().withGenres().build());

        MediaForm form = form().genreFacet().build();
        form.getFacets().getGenres().setSort(FacetOrder.VALUE_ASC);

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getGenres()).hasSize(2);
        final GenreFacetResultItem first = result.getFacets().getGenres().get(0);
        assertThat(first.getValue()).isEqualTo("Documentaire - Natuur");
        assertThat(first.getId()).isEqualTo("3.0.1.8.25");
        assertThat(first.getTerms()).hasSize(2);
    }

    @Test
    public void testFindWithGenreFacetWhenFiltered() throws Exception {
        index(program().withMid().genres(new Genre("3.0.1.1.6")).build());
        index(program().withMid().genres(new Genre("3.0.1.1.7")).build());

        MediaForm form = form().genreFacet().build();

        final MediaSearch search = new MediaSearch();
        search.setGenres(new TextMatcherList(new TextMatcher("3.0.1.1.6")));
        form.getFacets().getGenres().setFilter(search);

        MediaSearchResult result = target.find(null, form, 0, null);
        assertThat(result.getFacets().getGenres()).hasSize(1);
        assertThat(result.getFacets().getGenres().get(0).getValue()).isEqualTo("Jeugd - Amusement");
    }

    @Test
    public void testFindWithGenreFacetWithSubSearch() throws Exception {
        index(program().withMid().genres(new Genre("3.0.1.1.6")).build());
        index(program().withMid().genres(new Genre("3.0.1.1.7")).build());

        MediaForm form = form().genreFacet().build();

        final TermSearch search = new TermSearch();
        search.setIds(new TextMatcherList(new TextMatcher("3.0.1.1.6")));
        form.getFacets().getGenres().setSubSearch(search);

        MediaSearchResult result = target.find(null, form, 0, null);
        assertThat(result.getFacets().getGenres()).hasSize(1);
    }

    @Test
    public void testFindWithTagFacet() throws Exception {
        index(program().withMid().withTags().build());

        MediaForm form = form().tagFacet().build();

        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getTags()).hasSize(3);
    }

    @Test
    public void testFindWithTagFacetIgnoreCase() throws Exception {
        index(program().withMid().tags("foo", "bar").build());
        index(program().withMid().tags("FOO", "BAR").build());

        MediaForm form = form().tagFacet(false).build();
        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getTags()).hasSize(2);

    }

    @Test
    public void testFindWithTagFacetIgnoreCaseWithSelected() throws Exception {
        index(program().withMid().tags("foo", "bar").build());
        index(program().withMid().tags("FOO", "BAR").build());

        MediaForm form = form().tagFacet(false).tags(false, "foo").build();
        MediaSearchResult result = target.find(null, form, 0, null);

        assertThat(result.getFacets().getTags()).hasSize(2);
        assertThat(result.getFacets().getTags().get(1).getId()).isEqualTo("foo");
        assertThat(result.getFacets().getTags().get(1).isSelected()).isTrue();

        assertThat(result.getSelectedFacets().getTags()).hasSize(1);
        assertThat(result.getSelectedFacets().getTags().get(0).getId()).isEqualTo("foo");

    }

    @Test
    public void testFindWithMemberOfFacetWithAdditionalFields() throws Exception {
        final Group group = index(group().withMid().mainTitle("Group title").build());
        final Program program = index(program().withMid().memberOf(group, 1).build());

        when(target.mediaRepository.redirect(anyString())).thenReturn(Optional.empty());

        MediaForm form = form().memberOfFacet().build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MemberRefFacetResultItem> memberOf = result.getFacets().getMemberOf();

        assertThat(memberOf).isNotEmpty();
        assertThat(memberOf.get(0).getId()).isEqualTo(program.getMemberOf().first().getMidRef());
        assertThat(memberOf.get(0).getValue()).isEqualTo("Group title");
    }

    @Test
    public void testFindWithEpisodeOfFacet() throws Exception {
        index(program().withMid().type(ProgramType.BROADCAST).withEpisodeOf().build());

        MediaForm form = form().episodeOfFacet().build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MemberRefFacetResultItem> episodeOf = result.getFacets().getEpisodeOf();

        assertThat(episodeOf).isNotEmpty();
    }

    @Test
    public void testFindWithDescendantOfFacet() throws Exception {
        index(program().withMid().withDescendantOf().build());

        MediaForm form = form().descendantOfFacet().build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MemberRefFacetResultItem> descendantOf = result.getFacets().getDescendantOf();

        assertThat(descendantOf).isNotEmpty();
    }

    @Test
    public void testFindWithRelationFacet() throws Exception {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid().relations(new Relation(label, null, "Blue Note")).build());
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")).build());

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0)).hasSize(2);
    }

    @Test
    public void testFindWithRelationFacetCaseInsensitive() throws Exception {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");

        index(program().withMid().relations(new Relation(label, null, "Blue Note")).build());
        index(program().withMid().relations(new Relation(label, null, "blue note")).build());
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")).build());

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        relationFacet.setCaseSensitive(false);
        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0).getFacets()).hasSize(2);
        assertThat(relations.get(0).getFacets().get(0).getId()).isEqualTo("blue note");

    }

    @Test
    public void testFindWithRelationFacetAndSearchCaseInsensitive() throws Exception {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");

        index(program().withMid().relations(new Relation(label, null, "Blue Note")).build());
        index(program().withMid().relations(new Relation(label, null, "blue note")).build());
        index(program().withMid()
                .relations(new Relation(eoLabel, null, "Evangelisch"), new Relation(label, null, "Blue NOte")).build());

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        relationFacet.setCaseSensitive(false);

        MediaForm form = form().relationsFacet(relationFacet)
                .relationText(label, ExtendedTextMatcher.must("blue note", false)).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0).getFacets()).hasSize(2);
        assertThat(relations.get(0).getFacets().get(0).getId()).isEqualTo("blue note");
        assertThat(relations.get(0).getFacets().get(0).getCount()).isEqualTo(3);
        assertThat(relations.get(0).getFacets().get(0).isSelected()).isTrue();

        assertThat(relations.get(0).getFacets().get(1).getId()).isEqualTo("evangelisch");
        assertThat(relations.get(0).getFacets().get(1).getCount()).isEqualTo(1);
        assertThat(relations.get(0).getFacets().get(1).isSelected()).isFalse();

        assertThat(result.getSelectedFacets().getRelations()).hasSize(1);
        assertThat(result.getSelectedFacets().getRelations().get(0).getName()).isEqualTo("test");
        assertThat(result.getSelectedFacets().getRelations().get(0).getFacets()).hasSize(1);
        assertThat(result.getSelectedFacets().getRelations().get(0).getFacets().get(0).getId()).isEqualTo("blue note");
        assertThat(result.getSelectedFacets().getRelations().get(0).getFacets().get(0).getCount()).isEqualTo(3);

    }

    @Test
    public void testFindWithRelationFacetWithSearch() throws Exception {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid().relations(new Relation(label, null, "Blue Note")).build());
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")).build());

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        RelationSearch search = new RelationSearch();
        search.setBroadcasters(TextMatcherList.must(TextMatcher.must("VPRO")));
        relationFacet.setSubSearch(search);
        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0)).hasSize(1);
    }

    @Test
    public void testFindWithRelationFacetWithFilter() throws Exception {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid().broadcasters(new Broadcaster("VPRO"))
                .relations(new Relation(label, null, "Blue Note")).build());
        index(program().withMid().broadcasters(new Broadcaster("EO"))
                .relations(new Relation(eoLabel, null, "Evangelisch")).build());

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        MediaSearch search = new MediaSearch();
        search.setBroadcasters(TextMatcherList.must(TextMatcher.must("VPRO")));
        relationFacet.setFilter(search);
        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = target.find(null, form, 0, null);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0)).hasSize(1);
    }

    @Test
    public void testWithLocationFilter() throws IOException, ExecutionException, InterruptedException {

        index(program().build()); // no locations
        final Location location1 = new Location("http://www.locations.nl/1", OwnerType.BROADCASTER);
        location1.setId(1L);
        index(program().locations(location1).build()); // just a location with
                                                       // no platform
        final Location location2 = new Location("http://www.locations.nl/2", OwnerType.BROADCASTER,
                Platform.INTERNETVOD);
        location2.setId(2L);
        index(program().authoritativeRecord(Platform.INTERNETVOD).locations(location2).build()); // a location with  a specific platform

        {
            Filter filter = new Filter();
            HasLocationConstraint hasLocationConstraint = new HasLocationConstraint();
            hasLocationConstraint.setPlatform(Platform.INTERNETVOD.name());
            filter.setConstraint(hasLocationConstraint);
            ProfileDefinition<MediaObject> hasLocations = new ProfileDefinition<>(filter, null);
            JAXB.marshal(hasLocations, System.out);

            MediaSearchResult result = target.find(hasLocations, null, 0, null);
            assertThat(result.asList()).hasSize(1);
        }
        {
            Filter filter = new Filter();
            HasLocationConstraint hasLocationConstraint = new HasLocationConstraint();
            hasLocationConstraint.setPlatform("NONE");
            filter.setConstraint(hasLocationConstraint);
            ProfileDefinition<MediaObject> hasLocations = new ProfileDefinition<>(filter, null);
            JAXB.marshal(hasLocations, System.out);

            MediaSearchResult result = target.find(hasLocations, null, 0, null);
            assertThat(result.asList()).hasSize(1);
        }
        {
            Filter filter = new Filter();
            Or or = new Or();
            HasLocationConstraint hasLocationConstraint1 = new HasLocationConstraint();
            hasLocationConstraint1.setPlatform(Platform.INTERNETVOD.name());
            or.getConstraints().add(hasLocationConstraint1);
            HasLocationConstraint hasLocationConstraint2 = new HasLocationConstraint();
            hasLocationConstraint2.setPlatform("NONE");
            or.getConstraints().add(hasLocationConstraint2);

            filter.setConstraint(or);
            ProfileDefinition<MediaObject> hasLocations = new ProfileDefinition<>(filter, null);
            JAXB.marshal(hasLocations, System.out);

            MediaSearchResult result = target.find(hasLocations, null, 0, null);
            assertThat(result.asList()).hasSize(2);// FAILS NPA-298
        }
    }

    @Test
    public void testRedirectFormNull() {
        assertThat(target.redirectForm(null)).isNull();
    }

    @Test
    public void testRedirectFormWithMediaIds() {
        redirect("abc", "xyz");

        assertThat(target.redirectForm(form().mediaIds("abc", "def").build()).getSearches().getMediaIds().asList()
                .toString()).isEqualTo(
                        "[TextMatcher{value='xyz', match='SHOULD', matchType='TEXT'}, TextMatcher{value='def', match='SHOULD', matchType='TEXT'}]");

    }

    @Test
    public void testRedirectFormWithDescendantsOf() {
        redirect("abc", "xyz");
        assertThat(target.redirectForm(form().descendantOfs("abc", "def").build()).getSearches().getDescendantOf()
                .asList().toString()).isEqualTo(
                        "[TextMatcher{value='xyz', match='SHOULD', matchType='TEXT'}, TextMatcher{value='def', match='SHOULD', matchType='TEXT'}]");

    }

    @Test
    public void testRedirectFormWithEpisodeOf() {
        redirect("abc", "xyz");

        assertThat(target.redirectForm(form().episodeOfs("abc", "def").build()).getSearches().getEpisodeOf().asList()
                .toString()).isEqualTo(
                        "[TextMatcher{value='xyz', match='SHOULD', matchType='TEXT'}, TextMatcher{value='def', match='SHOULD', matchType='TEXT'}]");

    }

    @Test
    public void testRedirectFormWithMemberOf() {
        redirect("abc", "xyz");

        assertThat(target.redirectForm(MediaFormBuilder.form().memberOfs("abc", "def").build()).getSearches()
                .getMemberOf().asList().toString()).isEqualTo(
                        "[TextMatcher{value='xyz', match='SHOULD', matchType='TEXT'}, TextMatcher{value='def', match='SHOULD', matchType='TEXT'}]");

    }

    @Test
    public void testRedirectFormFilter() {
        redirect("abc", "xyz");

        MediaForm helper = form().memberOfs("abc", "def").build();

        MemberRefFacet facet = new MemberRefFacet();
        facet.setFilter(helper.getSearches());
        assertThat(target.redirectForm(form().memberOfFacet(facet).build()).getFacets().getMemberOf().getFilter()
                .getMemberOf().asList().toString()).isEqualTo(
                        "[TextMatcher{value='xyz', match='SHOULD', matchType='TEXT'}, TextMatcher{value='def', match='SHOULD', matchType='TEXT'}]");

    }

    @Test
    public void testListMembers() throws IOException, ExecutionException, InterruptedException {

        Group group = index(group().mid("MID_0").build());
        index(program().mid("MID_1").memberOf(group, 0).memberOf(group, 2).build());
        index(program().mid("MID_2").memberOf(group, 1).build());

        MediaResult result = target.listMembers(group, null, Order.ASC, 0L, 10);

        assertThat(result).hasSize(3);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_2");
        assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_1");

    }



    @Test
    public void testListMembers3WithProfile() throws IOException, ExecutionException, InterruptedException {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );
        Group group = index(group().mid("MID_0").build());
        Group unrelatedGroup = index(group().mid("MID_100").build());

        index(program().mid("MID_1").memberOf(group, 0).memberOf(group, 3).memberOf(unrelatedGroup, 3).broadcasters("BNN").build());
        index(program().mid("MID_2").memberOf(group, 1).build());
        index(program().mid("MID_3").memberOf(group, 2).broadcasters("BNN").build());


        {
            MediaResult result = target.listMembers(group, omroepProfile, Order.ASC, 0L, 10);

            assertThat(result).hasSize(3);
            assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
            assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_3");
            assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_1");
        }
        {
            MediaResult result = target.listMembers(unrelatedGroup, omroepProfile, Order.ASC, 0L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        }

    }


    @Test
    public void testListMembersWithProfileAndOffet() throws IOException, ExecutionException, InterruptedException {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );
        Group group = index(group().mid("MID_0").build());
        index(program().mid("MID_1").memberOf(group, 0).memberOf(group, 3).broadcasters("BNN").build());
        index(program().mid("MID_2").memberOf(group, 1).build());
        index(program().mid("MID_3").memberOf(group, 2).broadcasters("BNN").build());


        MediaResult result = target.listMembers(group, omroepProfile, Order.ASC, 1L, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_3");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_1");

    }


    @Test
    public void testListEpisodes() throws IOException, ExecutionException, InterruptedException {

        Group group = index(season().mid("MID_0").build());
        index(broadcast().mid("MID_1").episodeOf(group, 0).episodeOf(group, 2).build());
        index(broadcast().mid("MID_2").episodeOf(group, 1).build());
        index(broadcast().mid("MID_3").episodeOf(MemberRef.builder().owner(group).number(3).added(LocalDate.of(2017, 7, 12).atStartOfDay(Schedule.ZONE_ID).toInstant()).build()).build());
        index(broadcast().mid("MID_4").episodeOf(MemberRef.builder().owner(group).number(3).added(LocalDate.of(2017, 7, 11).atStartOfDay(Schedule.ZONE_ID).toInstant()).build()).build());
        index(broadcast().mid("MID_5").episodeOf(group, 4).build());

        ProgramResult result = target.listEpisodes(group, null, Order.ASC, 0L, 10);

        assertThat(result).hasSize(6);
        assertThat(result.getTotal()).isEqualTo(6);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_2");
        assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(3).getMid()).isEqualTo("MID_4");
        assertThat(result.getItems().get(4).getMid()).isEqualTo("MID_3");
        assertThat(result.getItems().get(5).getMid()).isEqualTo("MID_5");

    }


    @Test
    public void testListEpisodesWithProfile() throws IOException, ExecutionException, InterruptedException {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );

        Group group = index(season().mid("MID_0").build());
        index(broadcast().mid("MID_1").episodeOf(group, 0).episodeOf("MID_0", 2).broadcasters("BNN").build());
        index(broadcast().mid("MID_2").episodeOf(group, 1).build());
        index(broadcast().mid("MID_3").episodeOf(group, 3).broadcasters("BNN").build());

        ProgramResult result = target.listEpisodes(group, omroepProfile, Order.ASC, 0L, 10);

        assertThat(result).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_3");

    }


    @Test
    public void testListEpisodesWithProfileAndOffset() throws IOException, ExecutionException, InterruptedException {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );

        Group group = index(season().mid("MID_0").build());
        index(broadcast().mid("MID_1").episodeOf(group, 0).episodeOf("MID_0", 2).broadcasters("BNN").build());
        index(broadcast().mid("MID_2").episodeOf(group, 1).build());
        index(broadcast().mid("MID_3").episodeOf(group, 3).broadcasters("BNN").build());

        ProgramResult result = target.listEpisodes(group, omroepProfile, Order.ASC, 1L, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_3");

    }

    @Test
    public void testRedirect() throws IOException, ExecutionException, InterruptedException {
        settings.setRedirectsRepository("ELASTICSEARCH");
        Group group1 = index(group().published().mid("MID_0").build());
        Group group2 = index(group().mergedTo(group1).mid("MID_1").build());
        target.refillRedirectCache();
        Optional<String> result = target.redirect("MID_1");
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo("MID_0");
    }

    @Test
    public void testAgeRating() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").ageRating(_6).build());
        index(program().mainTitle("t2").ageRating(_12).build());
        index(program().mainTitle("t3").ageRating(ALL).build());

        {
            SearchResult<MediaObject> result = target.find(null, form().ageRating(_6).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getItems().get(0).getResult().getMainTitle()).isEqualTo("t1");
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().ageRating(_12).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
            assertEquals("t2", result.getItems().get(0).getResult().getMainTitle());
        }

        assertEquals(2L, (long) target.find(null, form().ageRating(_6, _12).build(), 0, null).getSize());
        assertEquals(1L, (long) target.find(null, form().ageRating(ALL).build(), 0, null).getSize());
        assertEquals(3L, (long) target.find(null, form().build(), 0, null).getSize());
    }

    @Test
    public void testGenreFilter() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").genres(new Genre("3.0.1.1.6")).build());

        {
            SearchResult<MediaObject> result = target.find(null, form().build(), 0, null);
            assertEquals(1, result.getSize().intValue());
            assertEquals("Jeugd - Amusement",
                    result.getItems().get(0).getResult().getGenres().first().getDisplayName());
            assertEquals("t1", result.getItems().get(0).getResult().getMainTitle());
        }

        index(program().mainTitle("t2").genres(new Genre("3.0.1.5")).build());

        ProfileDefinition<MediaObject> genreProfile = new ProfileDefinition<>(
                new Filter(new GenreConstraint("3.0.1.5")));
        {
            SearchResult<MediaObject> result = target.find(genreProfile, form().build(), 0, null);
            assertEquals(1, result.getSize().intValue());
            assertEquals("Muziek", result.getItems().get(0).getResult().getGenres().first().getDisplayName());
            assertEquals("t2", result.getItems().get(0).getResult().getMainTitle());

        }

    }


    @Test
    public void testGenreFilterWildCard() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").genres(new Genre("3.0.1.1.6")).build());
        index(program().mainTitle("t2").genres(new Genre("3.0.1.5")).build());
        index(program().mainTitle("t3").genres(new Genre("3.0.1")).build());
        index(program().mainTitle("t4").genres(new Genre("3.0.2")).build());

        ProfileDefinition<MediaObject> genreProfile = new ProfileDefinition<>(
            new Filter(new GenreConstraint("3.0.1*")));
        {
            SearchResult<MediaObject> result = target.find(genreProfile, form().build(), 0, null);
            assertEquals(3, result.getSize().intValue());
        }
    }

    @Test
    public void testContentRatings() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").contentRatings(ANGST).build());
        index(program().mainTitle("t2").contentRatings(DRUGS_EN_ALCOHOL).build());
        index(program().mainTitle("t3").contentRatings(ANGST, DRUGS_EN_ALCOHOL).build());

        assertEquals(3L, (long) target.find(null, form().build(), 0, null).getSize());
        assertEquals(2L, (long) target.find(null, form().contentRatings(ANGST).build(), 0, null).getSize());
        assertEquals(2L, (long) target.find(null, form().contentRatings(DRUGS_EN_ALCOHOL).build(), 0, null).getSize());
        assertEquals(2L,
                (long) target.find(null, form().contentRatings(DRUGS_EN_ALCOHOL, SEKS).build(), 0, null).getSize());
        assertEquals(0L, (long) target.find(null, form().contentRatings(SEKS).build(), 0, null).getSize());
        assertEquals(0L,
                (long) target.find(null, form().contentRatings(DISCRIMINATIE, SEKS).build(), 0, null).getSize());
    }

    @Test
    public void testAgeAndContentRatings() throws IOException, ExecutionException, InterruptedException {
        index(program().mainTitle("t1").contentRatings(SEKS).ageRating(_16).build());

        assertEquals(1L, (long) target.find(null, form().build(), 0, null).getSize());
        assertEquals(1L, (long) target.find(null, form().contentRatings(SEKS).build(), 0, null).getSize());
        assertEquals(1L, (long) target.find(null, form().ageRating(_16).build(), 0, null).getSize());
        assertEquals(1L,
                (long) target.find(null, form().contentRatings(SEKS).ageRating(_16).build(), 0, null).getSize());
        assertEquals(1L, (long) target
                .find(null, form().contentRatings(DISCRIMINATIE, SEKS).ageRating(_12, _16).build(), 0, null).getSize());
        assertEquals(0L, (long) target.find(null, form().contentRatings(ANGST).build(), 0, null).getSize());
        assertEquals(0L, (long) target.find(null, form().ageRating(_12).build(), 0, null).getSize());
    }

    @Test
    public void testAgeRatingWithFacets() throws Exception {
        index(program().mainTitle("t1").ageRating(_12).build());
        index(program().mainTitle("t2").ageRating(_12).build());
        index(program().mainTitle("t3").ageRating(_6).build());
        index(program().mainTitle("t4").ageRating(_6).build());
        index(program().mainTitle("t5").ageRating(_6).build());
        index(program().mainTitle("t6").ageRating(ALL).build());

        MediaSearchResult result = target.find(null, form().ageRating(ALL, _6, _12).ageRatingFacet(0).build(), 0, null);
        List<TermFacetResultItem> ageRatings = result.getFacets().getAgeRatings();

        assertEquals(5, ageRatings.size());

        /* 3 x 6, 0 x 9, 2 * 12, 0 x 16, 1 * Alle Leeftijden */
        assertEquals(3, ageRatings.get(0).getCount());
        assertEquals(_6.getXmlValue(), ageRatings.get(0).getId());
        assertEquals(0, ageRatings.get(1).getCount());
        assertEquals(_9.getXmlValue(), ageRatings.get(1).getId());
        assertEquals(2, ageRatings.get(2).getCount());
        assertEquals(_12.getXmlValue(), ageRatings.get(2).getId());
        assertEquals(0, ageRatings.get(3).getCount());
        assertEquals(_16.getXmlValue(), ageRatings.get(3).getId());
        assertEquals(1, ageRatings.get(4).getCount());
        assertEquals(ALL.getXmlValue(), ageRatings.get(4).getId());
        assertEquals(ALL.getDisplayName(), ageRatings.get(4).getValue());

        {
            MediaSearchResult result1 = target.find(null, form().ageRating(_16).ageRatingFacet().build(), 0, null);
            assertEquals(1L, (long) result1.getSelectedFacets().getAgeRatings().size());
            assertEquals(_16.getXmlValue(), result1.getSelectedFacets().getAgeRatings().get(0).getValue());
            assertEquals(0, result1.getSelectedFacets().getAgeRatings().get(0).getCount());
        }
    }

    @Test
    public void testContentRatingWithFacets() throws Exception {
        index(program().mainTitle("t1").contentRatings(DISCRIMINATIE, DRUGS_EN_ALCOHOL, ANGST).build());
        index(program().mainTitle("t2").contentRatings(SEKS).build());
        index(program().mainTitle("t3").contentRatings(DRUGS_EN_ALCOHOL).build());
        index(program().mainTitle("t4").contentRatings(ANGST).build());
        index(program().mainTitle("t5").contentRatings(GEWELD, GROF_TAALGEBRUIK).build());
        index(program().mainTitle("t6").contentRatings(GROF_TAALGEBRUIK).build());

        MediaSearchResult result = target.find(null,
                form().contentRatings(DISCRIMINATIE, SEKS, DRUGS_EN_ALCOHOL, ANGST, GEWELD, GROF_TAALGEBRUIK)
                        .contentRatingsFacet().build(),
                0, null);
        List<TermFacetResultItem> contentRatings = result.getFacets().getContentRatings();
        /*
         * ANGST: 2, DISCRIMINATIE: 1, DRUGS_EN_ALCOHOL: 2, GEWELD: 1,
         * GROF_TAALGEBRUIK: 2, SEKS: 1, alphabetically
         */
        assertEquals(contentRatings.size(), ContentRating.values().length);

        assertEquals(ANGST.name(), contentRatings.get(0).getId());
        assertEquals(2, contentRatings.get(0).getCount());
        assertEquals(DISCRIMINATIE.name(), contentRatings.get(1).getId());
        assertEquals(1, contentRatings.get(1).getCount());
        assertEquals(DRUGS_EN_ALCOHOL.name(), contentRatings.get(2).getId());
        assertEquals(2, contentRatings.get(2).getCount());
        assertEquals(GEWELD.name(), contentRatings.get(3).getId());
        assertEquals(1, contentRatings.get(3).getCount());
        assertEquals(GROF_TAALGEBRUIK.name(), contentRatings.get(4).getId());
        assertEquals(2, contentRatings.get(4).getCount());
        assertEquals(SEKS.name(), contentRatings.get(5).getId());
        assertEquals(1, contentRatings.get(5).getCount());
    }

    @Test
    public void testFindWithAgeRatingProfile() throws Exception {
        index(program().mainTitle("sex!").contentRatings(SEKS).ageRating(_16).build());
        index(program().mainTitle("heel gewoon").build());

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
                new Filter(new Or(new Not(new HasAgeRatingConstraint()), new AgeRatingConstraint(AgeRating.ALL),
                        new AgeRatingConstraint(AgeRating._9), new AgeRatingConstraint(AgeRating._6))));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithContentRatingsProfile() throws Exception {
        index(program().mainTitle("sex!").contentRatings(SEKS).ageRating(_16).build());
        index(program().mainTitle("heel gewoon").build());

        ProfileDefinition<MediaObject> pornoSiteProfile = new ProfileDefinition<>(
                new Filter(new ContentRatingConstraint(SEKS)));
        ProfileDefinition<MediaObject> childrenSiteProfile = new ProfileDefinition<>(
                new Filter(new Or(new Not(new HasAgeRatingConstraint()), new Not(new ContentRatingConstraint(SEKS)))));

        {
            SearchResult<MediaObject> result = target.find(pornoSiteProfile, null, 0, null);
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getItems().get(0).getResult().getMainTitle()).isEqualTo("sex!");
        }
        {
            SearchResult<MediaObject> result = target.find(childrenSiteProfile, null, 0, null);
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getItems().get(0).getResult().getMainTitle()).isEqualTo("heel gewoon");
        }
    }

    @Test
    // NPA-403
    public void testSortByTitles() throws IOException, ExecutionException, InterruptedException {
        index(program()
            .mainTitle("aa")
            .lexicoTitle("bb")
            .mid("aa")
            .build());
        index(program()
            .mainTitle("bb")
            .lexicoTitle("aa")
            .mid("bb")
            .build());

        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder().textualType(TextualType.MAIN).order(Order.ASC).build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("aa");
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("bb");
        }
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder().textualType(TextualType.LEXICO).order(Order.ASC).build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("bb");
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("aa");
        }
    }


    @Test
    // NPA-403
    public void testSortByLexico() throws IOException, ExecutionException, InterruptedException {
        index(program()
            .mainTitle("bb")
            //.lexicoTitle("bb") Should be implicit
            .mid("bb")
            .build());
        index(program()
            .mainTitle("aa")
            .lexicoTitle("cc")
            .mid("aa")
            .build());
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("bb");
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("aa");
        }
    }


    @Test
    // NPA-403
    public void testSortByLexicoForOwner() throws IOException, ExecutionException, InterruptedException {
        index(program()
            .mainTitle("bbmis", OwnerType.MIS)
            .mainTitle("cc", OwnerType.BROADCASTER)
            .mid("bb")
            .build());
        index(program()
            .mainTitle("ccmis", OwnerType.MIS)
            .mainTitle("bb", OwnerType.BROADCASTER)
            .lexicoTitle("ccmislexico", OwnerType.NPO)
            .lexicoTitle("bblexico", OwnerType.BROADCASTER)
            .mid("aa")
            .build());
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .ownerType(OwnerType.MIS)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("bb");
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("aa");
        }
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .ownerType(OwnerType.BROADCASTER)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("aa");
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("bb");
        }
    }

    @Test
    @Ignore("Doesn't test a thing yet. TODO: we might introduce a search on title feature?")
    public void testFindByTitles() throws InterruptedException, ExecutionException, IOException {
        index(program()
            .mainTitle("abcde", OwnerType.BROADCASTER)
            .mid("abcde")
            .build());
        index(program()
            .mainTitle("aaaaa")
            .mid("aa")
            .build());
        index(program()
            .mainTitle("bbbbb")
            .mid("bb")
            .build());

        MediaForm form = MediaForm.builder()
            .fuzzyText("aaa")
            .build();

        SearchResult<MediaObject> result = target.find(null, form, 0, null);

        log.info("{}", result);
    }

    private void redirect(String from, String to) {
        Map<String, String> redirects = new HashMap<>();
        redirects.put(from, to);
        when(target.mediaRepository.redirects()).thenReturn(new RedirectList(null, null, redirects));
    }

    private <T extends MediaObject> T index(T object) throws IOException, ExecutionException, InterruptedException {
        indexMediaObject(object);
        for (MemberRef ref : object.getMemberOf()) {
            String memberRefType = MediaESType.memberRef(ref.getOwner().getClass()).name();
            index(memberRefType, object, ref);
        }
        if (object instanceof Program) {
            for (MemberRef ref : ((Program) object).getEpisodeOf()) {
                index(MediaESType.episodeRef.name(), object, ref);
            }
        }
        return object;
    }

    private void indexMediaObject(MediaObject object) throws IOException, ExecutionException, InterruptedException {
        byte[] bytes = Jackson2Mapper.getPublisherInstance().writeValueAsBytes(object);
        client.index(new IndexRequest(indexName, getTypeName(object), object.getMid())
            .source(bytes, XContentType.JSON))
            .actionGet();
        refresh();

    }

    private void index(String type, MediaObject child, MemberRef object) throws IOException, ExecutionException, InterruptedException {
        StandaloneMemberRef ref = StandaloneMemberRef.builder()
            .childRef(child.getMid())
            .memberRef(object)
            .build();
        byte[] bytes = Jackson2Mapper.getPublisherInstance().writeValueAsBytes(ref);
        client.index(new IndexRequest(indexName, type, ref.getId())
                .source(bytes, XContentType.JSON)
                .parent(object.getMidRef()))
            .actionGet();
        log.info("Indexed {} {}", type, ref.getId());
        refresh();
    }

}
