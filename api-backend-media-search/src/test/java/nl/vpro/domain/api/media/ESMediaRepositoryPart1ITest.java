package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.domain.Displayable;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.*;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.Portal;
import nl.vpro.elasticsearch.Constants;
import nl.vpro.elasticsearchclient.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.ApiRefsIndex;

import static nl.vpro.domain.api.FacetOrder.VALUE_ASC;
import static nl.vpro.domain.api.Match.MUST;
import static nl.vpro.domain.api.StandardMatchType.REGEX;
import static nl.vpro.domain.api.StandardMatchType.WILDCARD;
import static nl.vpro.domain.api.TextMatcher.must;
import static nl.vpro.domain.api.media.MediaFormBuilder.form;
import static nl.vpro.domain.media.AgeRating.*;
import static nl.vpro.domain.media.Channel.NED2;
import static nl.vpro.domain.media.ContentRating.*;
import static nl.vpro.domain.media.GeoRoleType.SUBJECT;
import static nl.vpro.domain.media.MediaTestDataBuilder.*;
import static nl.vpro.domain.media.Schedule.ZONE_ID;
import static nl.vpro.domain.media.StandaloneMemberRef.ObjectType.episodeRef;
import static nl.vpro.domain.media.StandaloneMemberRef.ObjectType.memberRef;
import static nl.vpro.domain.media.support.OwnerType.*;
import static nl.vpro.domain.media.support.TextualType.MAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * See also {@link ESMediaRepositoryPart2ITest} This test sets up de index for
 * every test. Part2 creates a bunch of test data in @Setup. Choose what is more
 * convenient for your new tests.
 *
 * @author Roelof Jan Koekoek
 * @since 3.5
 */
@ContextConfiguration(locations = "classpath:nl/vpro/domain/api/media/ESMediaRepositoryITest-context.xml")
@Slf4j
public class ESMediaRepositoryPart1ITest extends AbstractMediaESRepositoryITest {

    @Autowired
    private ESMediaRepository target;

    @Override
    protected void firstRun() {
        createIndicesIfNecessary();
    }
    @BeforeEach
    public  void setup() {
        target.setIndexName(indexHelpers.get(ApiMediaIndex.APIMEDIA).getIndexName());
        target.redirects = new RedirectList();
        clearIndices();
    }

    @AfterEach
    public void checkScrollIds() {
        assertThat(ElasticSearchIterator.getScrollIds()).isEmpty();
    }



    @Test
    public void testLoad() {
        index(program().mainTitle("foo bar").mid("MID_FOR_LOAD"));
        target.setScore(false);
        MediaObject result = target.load("MID_FOR_LOAD");
        assertThat(result.getMainTitle()).isEqualTo("foo bar");
        assertThat(result.getMid()).isEqualTo("MID_FOR_LOAD");

    }

    @Test
    public void testText() {
        index(program().mainTitle("foo"));
        index(program().mainTitle("bar"));

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
    public void testTextFuzzy() {
        index(program().mainTitle("foo"));
        index(program().mainTitle("foa"));
        index(program().mainTitle("bar"));
        index(program().mainTitle("Tegenlicht"));

        {
            SearchResult<MediaObject> result = target.find(null, form().fuzzyText("foa").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            List<? extends SearchResultItem<? extends MediaObject>> items = result.getItems();
            assertThat(items.get(1).getScore()).isLessThanOrEqualTo(items.get(0).getScore());
            assertThat(items.stream().anyMatch(item -> item.getResult().getMainTitle().equals("foo"))).isTrue();
            assertThat(items.stream().anyMatch(item -> item.getResult().getMainTitle().equals("foa"))).isTrue();
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().text("FOO").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }
        {
            SearchResult<MediaObject> result = target.find(null, form().fuzzyText("Regenlucht").build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }
    }


    @Test
    public void testResultOrderTextSearch() {
        target.setScore(true);
        index(program().mainTitle("De Ideale Wereld").tags("wereld")); // scores best, it also has a tag
        index(program().mainTitle("Alleen op de Wereld"));
        //index(program().mainTitle("Het Wereldje Draait Door").build()); // scores lowest, because it matched only approximately
        // Sadly: 'wereldje' is tokenized as:

        /*
         curl http://localhost:9200/test-apimedia-2018-06-2913:49:03/_analyze -d '{
  "analyzer": "dutch_stemmed",
  "text":      "wereldje?"
}
'
{"tokens":[{"token":"wereldj","start_offset":0,"end_offset":8,"type":"<ALPHANUM>","position":0}]}
         */

        // I don't quite see how it makes sense either.

        index(program().mainTitle("De Werelden Draaien Door")); // scores lowest, because it matched only approximately


        {
            SearchResult<MediaObject> result = target.find(null, form().text("WERELD").build(), 0, null);
            log.info("{}", result.getItems());
            assertThat(result.getSize()).isEqualTo(3);
            float score0 = result.getItems().get(0).getScore();
            float score1 = result.getItems().get(1).getScore();
            float score2 = result.getItems().get(2).getScore();
            assertThat(score0).isGreaterThan(score1);
            assertThat(score1).isGreaterThanOrEqualTo(score2);
            //assertThat(score1).isGreaterThan(score2);
            assertThat(result.getItems().get(0).getResult().getMainTitle()).isEqualTo("De Ideale Wereld");
            //assertThat(result.getItems().get(1).getResult().getMainTitle()).isEqualTo("Alleen op de Wereld");
            //assertThat(result.getItems().get(2).getResult().getMainTitle()).isEqualTo("De Wereld Draait Door");

        }
    }

    @Test
    public void testFindTagText() {
        index(program().mainTitle("t1").tags("foo", "bar"));
        index(program().mainTitle("t2").tags("xxx", "yyy"));

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
                    form().tags(MUST, ExtendedTextMatcher.must("FOO", false)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

    }

    @Test
    public void testFindTagTextRegex() {
        index(program().mainTitle("t1").tags("foo", "bar"));
        index(program().mainTitle("t2").tags("xxx", "yyy"));

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("fo.*", REGEX)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("FO.*", REGEX)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.must("FO.*", REGEX, false)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

    }



    @Test
    public void testFindTagWildcard() {
        index(program().mainTitle("t1").tags("foobar", "xxxyyyy"));
        index(program().mainTitle("t2").tags("xxx", "yyy"));

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("fo*bar", WILDCARD)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(1);
        }

        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("FO*BAR", WILDCARD)).build(), 0, null);
            assertThat(result.getSize()).isEqualTo(0);
        }
        {
            SearchResult<MediaObject> result = target.find(null,
                    form().tags(ExtendedTextMatcher.should("FO*BAR", WILDCARD, false)).build(), 0,
                    null);
            assertThat(result.getSize()).isEqualTo(1);
        }
    }

    @Test
    public void testFindWithHasImageProfile() {
        index(program().withMid());

        final Program withImages = program().withMid().withImages().build();
        withImages.getImages().get(0).setId(2L);
        index(MediaBuilder.program(withImages));

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(new Filter(new HasImageConstraint()));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithHasLocationsProfile() {
        index(program().withMid());

        final Program withLocations = program().withMid().withLocations().build();
        withLocations.getLocations().first().setId(2L);
        index(MediaBuilder.<ProgramBuilder, Program>of(withLocations));

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(new Filter(new HasLocationConstraint()));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithFacetOrderings() {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("A"), new Broadcaster("B")));

        {
            MediaForm countAsc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.COUNT_ASC, null)).build();
            MediaSearchResult result = getAndTestResult(countAsc);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
        }

        {
            MediaForm countDesc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.COUNT_DESC, null)).build();
            MediaSearchResult result = getAndTestResult(countDesc);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("B");
        }

        {
            MediaForm valueAsc = form().broadcasterFacet(new MediaFacet(null, VALUE_ASC, null)).build();
            MediaSearchResult result = getAndTestResult(valueAsc);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
        }

        {
            MediaForm valueDesc = form().broadcasterFacet(new MediaFacet(null, FacetOrder.VALUE_DESC, null)).build();
            MediaSearchResult result = getAndTestResult(valueDesc);
            assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("B");
        }
    }

    @Test
    public void testFindWithFacetWithThreshold() {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("B")));

        index(program().withMid().broadcasters(new Broadcaster("A")));

        MediaForm form = form().broadcasterFacet(new MediaFacet(2, VALUE_ASC, null)).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getBroadcasters()).hasSize(1);
        assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
    }

    @Test
    public void testFindWithFacetWithMax() {
        index(program().withMid().broadcasters(new Broadcaster("A"), new Broadcaster("B")));

        MediaForm form = form().broadcasterFacet(new MediaFacet(null, VALUE_ASC, 1)).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getBroadcasters()).hasSize(1);
        assertThat(result.getFacets().getBroadcasters().get(0).getId()).isEqualTo("A");
    }

    @Test
    public void testFindWithAvTypeFacet() {
        index(program().withMid().withAVType());

        MediaForm form = form().avTypes(AVType.VIDEO).avTypeFacet().build();
        MediaSearchResult result = getAndTestResult(form);

        //MediaSearchResult result = target.find(null, form().avTypes(AVType.VIDEO).avTypeFacet().build(), 0, null);

        assertThat(result.getFacets().getAvTypes()).isNotEmpty();
        List<TermFacetResultItem> avTypes = result.getFacets().getAvTypes();
        for (TermFacetResultItem avType : avTypes) {
            if (avType.getId().equals(AVType.VIDEO.name())) {
                assertThat(avType.getCount()).isEqualTo(1);
            } else {
                assertThat(avType.getCount()).isEqualTo(0);
            }
        }
    }

    @Test
    public void testFindWithTypeFacet() {
        index(program().withMid().withType());

        MediaForm form = form().typeFacet().build();

        MediaSearchResult result = getAndTestResult(form);

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
    public void testFindWithSortDateFacet() {
        index(program().withMid().withPublishStart());

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

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getSortDates()).isNotEmpty();
    }


    @Test
    public void testFindWithSortDateFacetMonth() {
        index(program().withMid().withPublishStart());

        // both ranges
        MediaForm form = form().sortDateFacet(
            new DateRangeInterval(1, IntervalUnit.MONTH)
        ).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getSortDates()).isNotEmpty();
    }

    @Test
    public void testFindWithSortDateFacetHistogram() {
        index(program().withMid().withPublishStart());

        // both ranges
        MediaForm form = form().sortDateFacet(
            new DateRangeInterval("1 year")
        ).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getSortDates()).isNotEmpty();
    }

    @Test
    public void testFindWithDurationFacetHistogram() {
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().duration(Duration.of(2, ChronoUnit.HOURS)));

        DurationRangeInterval interval = new DurationRangeInterval("1 hour");
        MediaForm form = form()
            .durationFacet(interval)
            .build();

        MediaSearchResult result = getAndTestResult(form);

        for (SearchResultItem<? extends MediaObject> mo : result) {
            log.info("{} {}", mo.getResult().getMid(), mo.getResult().getDuration());
        }

        assertThat(result.getFacets().getDurations()).isNotEmpty();
        assertThat(result.getFacets().getDurations()).hasSize(2);

        assertThat(result.getFacets().getDurations().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getDurations().get(1).getCount()).isEqualTo(1);
    }

    @Test
    public void testFindWithDurationFacetHistogramAndProfile() {
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().portalRestrictions(PortalRestriction.builder().portal(Portal.builder().id("eo").build()).build()).duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().duration(Duration.of(2, ChronoUnit.HOURS)));

        DurationRangeInterval interval = new DurationRangeInterval("1 hour");
        MediaForm form = form()
            .durationFacet(interval)
            .build();


        ProfileDefinition<MediaObject> notExclusive = new ProfileDefinition<>(
            new Filter(new Not(new HasPortalRestrictionConstraint()))
        );
        MediaSearchResult result =  target.find(notExclusive, form, 0, null);


        for (SearchResultItem<? extends MediaObject> mo : result) {
            log.info("{} {}", mo.getResult().getMid(), mo.getResult().getDuration());
        }

        assertThat(result.getFacets().getDurations()).isNotEmpty();
        assertThat(result.getFacets().getDurations()).hasSize(2);

        assertThat(result.getFacets().getDurations().get(0).getCount()).isEqualTo(1);
        assertThat(result.getFacets().getDurations().get(1).getCount()).isEqualTo(1);
    }



    @Test
    public void testFindWithDurationFacet() {
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().duration(Duration.of(1, ChronoUnit.HOURS)));
        index(program().withMid().duration(Duration.of(3, ChronoUnit.HOURS)));

        MediaForm form = form()
            .durationFacet(DurationRangeFacetItem.builder()
                .name("less than 2 hours")
                .end(Duration.ofHours(2))
                .build(),
            DurationRangeFacetItem.builder()
                .name("more than 2 hours")
                .begin(Duration.ofHours(2))
                .build()
        ).build();

        MediaSearchResult result = getAndTestResult(form);

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
    public void testFindWithGenreWildcard() {
        index(program().withMid().genres(new Genre("3.0.1.1.6")));
        index(program().withMid().genres(new Genre("3.0.1.1.7")));
        index(program().withMid().genres(new Genre("3.0.1.2.7")));


        MediaForm form = form()
            .genres(MUST, new TextMatcher("3.0.1.1.*", MUST, WILDCARD)).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(2);


    }

    @Test
    public void testFindWithGenreWildcardNothing() {
        index(program().withMid().genres(new Genre("3.0.1.1.6")));
        index(program().withMid().genres(new Genre("3.0.1.1.7")));
        index(program().withMid().genres(new Genre("3.0.1.2.7")));


        MediaForm form = form().genres(MUST, new TextMatcher("4.0.1.*", MUST, WILDCARD)).build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getSize()).isEqualTo(0);


    }


    @Test
    public void testFindWithGenreFacet() {
        target.setScore(false);

        index(program().withMid().withGenres());

        MediaForm form = form().genreFacet().build();
        form.getFacets().getGenres().setSort(VALUE_ASC);

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getGenres()).hasSize(2);
        final GenreFacetResultItem first = result.getFacets().getGenres().get(0);
        assertThat(first.getValue()).isEqualTo("Documentaire - Natuur");
        assertThat(first.getId()).isEqualTo("3.0.1.8.25");
        assertThat(first.getTerms()).hasSize(2);
    }

    @Test
    public void testFindWithGenreFacetWhenFiltered() {
        target.setScore(false);

        index(program().withMid().genres(new Genre("3.0.1.1.6")));
        index(program().withMid().genres(new Genre("3.0.1.1.6"), new Genre("3.0.1.1.5")));
        index(program().withMid().genres(new Genre("3.0.1.1.7")));

        MediaForm form = form().genreFacet().build();

        // This is actually a bit of an degenerated example. why would you filter the facet on exactly one other genre?
        // But anyhow this means: aggregate genre buckets on all documents wich at least have 3.0.1.1.6
        final MediaSearch search = new MediaSearch();
        search.setGenres(new TextMatcherList(new TextMatcher("3.0.1.1.6")));
        form.getFacets().getGenres().setFilter(search);

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getGenres()).hasSize(2);
        assertThat(result.getFacets().getGenres().get(0).getValue()).isEqualTo("Jeugd - Amusement");
        assertThat(result.getFacets().getGenres().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getGenres().get(1).getId()).isEqualTo("3.0.1.1.5");
    }

    @Test
    public void testFindWithGenreFacetWithSubSearch() {
        target.setScore(false);

        index(program().withMid().genres(new Genre("3.0.1.1.6")));
        index(program().withMid().genres(new Genre("3.0.1.1.6"), new Genre("3.0.1.1.5")));
        index(program().withMid().genres(new Genre("3.0.1.1.7")));

        MediaForm form = form().genreFacet().build();

        // This is a subsearch, so this means that we are only interested in the count for the given genres.
        final TermSearch search = new TermSearch();
        search.setIds(new TextMatcherList(new TextMatcher("3.0.1.1.6")));
        // Since this matched only one, we will get only one facet result!
        form.getFacets().getGenres().setSubSearch(search);

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getGenres()).hasSize(1);
        assertThat(result.getFacets().getGenres().get(0).getId()).isEqualTo("3.0.1.1.6");
        assertThat(result.getFacets().getGenres().get(0).getCount()).isEqualTo(2);

    }

    @Test
    public void testFindWithTagFacet() {
        index(program().withMid().withTags());

        MediaForm form = form().tagFacet().build();

        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getTags()).hasSize(3);
    }

    @Test
    public void testFindWithTagFacetIgnoreCase() {
        index(program().withMid().tags("foo", "bar"));
        index(program().withMid().tags("FOO", "BAR"));

        MediaForm form = form().tagFacet(false).build();
        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getTags()).hasSize(2);

    }

    @Test
    public void testFindWithTagFacetIgnoreCaseWithSelected() {
        index(program().withMid().tags("foo", "bar"));
        index(program().withMid().tags("FOO", "BAR"));

        MediaForm form = form().tagFacet(false).tags(false, "foo").build();
        MediaSearchResult result = getAndTestResult(form);

        assertThat(result.getFacets().getTags()).hasSize(2);
        assertThat(result.getFacets().getTags().get(1).getId()).isEqualTo("foo");
        assertThat(result.getFacets().getTags().get(1).isSelected()).isTrue();

        assertThat(result.getSelectedFacets().getTags()).hasSize(1);
        assertThat(result.getSelectedFacets().getTags().get(0).getId()).isEqualTo("foo");

    }

    @Test
    public void testFindWithMemberOfFacetWithAdditionalFields() {
        target.setScore(false);

        final Group group = index(group().withMid().mainTitle("Group title"));
        final Program program = index(program().withMid().memberOf(group, 1));

        MediaForm form = form().memberOfFacet().build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MemberRefFacetResultItem> memberOf = result.getFacets().getMemberOf();

        assertThat(memberOf).isNotEmpty();
        assertThat(memberOf.get(0).getId()).isEqualTo(program.getMemberOf().first().getMidRef());
        assertThat(memberOf.get(0).getValue()).isEqualTo("Group title");
    }

    @Test
    public void testFindWithEpisodeOfFacet() {
        index(program().withMid().type(ProgramType.BROADCAST).withEpisodeOf());

        MediaForm form = form().episodeOfFacet().build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MemberRefFacetResultItem> episodeOf = result.getFacets().getEpisodeOf();

        assertThat(episodeOf).isNotEmpty();
    }

    @Test
    public void testFindWithDescendantOfFacet() {
        index(program().withMid().withDescendantOf());

        MediaForm form = form().descendantOfFacet().build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MemberRefFacetResultItem> descendantOf = result.getFacets().getDescendantOf();

        assertThat(descendantOf).isNotEmpty();
    }

    @Test
    public void testFindWithRelationFacet() {
        target.setScore(false);

        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid().relations(new Relation(label, null, "Blue Note")));
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")));

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("my_relationsfacets");
        MediaForm form = form().relationsFacet(relationFacet).build();


        MediaSearchResult result = getAndTestResult(form);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("my_relationsfacets");
        assertThat(relations.get(0)).hasSize(2);
    }

    @Test
    public void testFindWithRelationFacetCaseInsensitive() {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");

        index(program().withMid().relations(new Relation(label, null, "Blue Note")));
        index(program().withMid().relations(new Relation(label, null, "blue note")));
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")));

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        relationFacet.setCaseSensitive(false);
        // relation facet with no subsearch, this does not really make much sense of course, but we support it anyways.

        MediaForm form = form()
            .relationsFacet(relationFacet)
            .build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0).getFacets()).hasSize(2);
        assertThat(relations.get(0).getFacets().get(0).getId()).isEqualTo("blue note");

    }

    @Test
    public void testFindWithRelationFacetAndSearchCaseInsensitive() {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");


        // 3 object
        // 3 have a vpro:label
        // 1 has eo:label
        // 3 have vpro:label blue note
        index(program().withMid().relations(
            Relation.ofText(label,"Blue Note")
        ));
        index(program().withMid().relations(
            Relation.ofText(label, "blue note")
        ));
        index(program().withMid().relations(
            Relation.ofText(eoLabel, "Evangelisch"),
            Relation.ofText(label, "Blue NOte")
        ));


        {
            RelationFacet relationFacet = new RelationFacet();
            relationFacet.setName("test");
            relationFacet.setCaseSensitive(false);

            MediaForm form = form()
                .relationsFacet(relationFacet)
                .relationText(label, ExtendedTextMatcher.must("blue note", false))
                .build();

            MediaSearchResult result = getAndTestResult(form);

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

    }
    @Test
    public void testFindWithRelationFacetWithSubSearchAndSearchCaseInsensitive() {
        target.setScore(false);
        RelationDefinition vproLabel = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");


        // 4 object
        // 4 have a vpro:label
        // 2 has eo:label
        // 3 have vpro:label blue note
        // 1 has vpro:label red note
        index(program().withMid().relations(
            Relation.ofText(vproLabel,"Blue Note")
        ));
        index(program().withMid().relations(
            Relation.ofText(vproLabel, "blue note")
        ));
        index(program().withMid().relations(
            Relation.ofText(eoLabel, "Evangelisch"),
            Relation.ofText(vproLabel, "Blue NOte")
        ));
        index(program().withMid().relations(
            Relation.ofText(eoLabel, "bla bla"),
            Relation.ofText(vproLabel, "blUe NOte"),
            Relation.ofText(vproLabel, "red note")
        ));

        {
            RelationFacet relationFacet = new RelationFacet();
            relationFacet.setName("test");
            relationFacet.setCaseSensitive(false);
            relationFacet.setSubSearch(
                RelationSearch.builder()
                    .broadcasters(TextMatcherList.must(must(vproLabel.getBroadcaster())))
                    .types(TextMatcherList.must(must(vproLabel.getType())))
                    .build()
            );

            MediaForm form = form()
                .relationsFacet(relationFacet)
                .relationText(vproLabel, ExtendedTextMatcher.must("blue note", false))
                .build();

            MediaSearchResult result = getAndTestResult(form);

            final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

            assertThat(relations).hasSize(1);
            assertThat(relations.get(0).getName()).isEqualTo("test");
            assertThat(relations.get(0).getFacets()).hasSize(2);
            assertThat(relations.get(0).getFacets().get(0).getId()).isEqualTo("blue note");
            assertThat(relations.get(0).getFacets().get(0).getCount()).isEqualTo(4);
            assertThat(relations.get(0).getFacets().get(0).isSelected()).isTrue();


            // In 5.4 this fails, it gives 'evangelisch'.
            // which is incorrect, since the relation search limit on vproLabels.
            assertThat(relations.get(0).getFacets().get(1).getId()).isEqualTo("red note");
            assertThat(relations.get(0).getFacets().get(1).getCount()).isEqualTo(1);
            assertThat(relations.get(0).getFacets().get(1).isSelected()).isFalse();

            assertThat(result.getSelectedFacets().getRelations()).hasSize(1);
            assertThat(result.getSelectedFacets().getRelations().get(0).getName()).isEqualTo("test");
            assertThat(result.getSelectedFacets().getRelations().get(0).getFacets()).hasSize(1);
            assertThat(result.getSelectedFacets().getRelations().get(0).getFacets().get(0).getId()).isEqualTo("blue note");
            assertThat(result.getSelectedFacets().getRelations().get(0).getFacets().get(0).getCount()).isEqualTo(4);
        }

    }

    @Test
    public void testFindWithRelationFacetWithSearch() {
        target.setScore(false);
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid()
            .relations(
                new Relation(label, null, "Blue Note"),
                new Relation(eoLabel, null, "Genesis")
            ));
        index(program().withMid().relations(new Relation(eoLabel, null, "Evangelisch")));

        RelationFacet relationFacet = RelationFacet.builder()
            .name("test")
            .build();

        RelationSearch search = new RelationSearch();
        search.setBroadcasters(TextMatcherList.must(TextMatcher.must("VPRO")));
        relationFacet.setSubSearch(search);

        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0)).hasSize(1);
        assertThat(relations.get(0).getFacets().get(0).getId()).isEqualTo("Blue Note");
        assertThat(relations.get(0).getFacets().get(0).getCount()).isEqualTo(1);

    }

    @Test
    public void testFindWithRelationFacetWithFilter() {
        RelationDefinition label = new RelationDefinition("label", "VPRO");
        RelationDefinition eoLabel = new RelationDefinition("label", "EO");
        index(program().withMid().broadcasters(new Broadcaster("VPRO"))
                .relations(new Relation(label, null, "Blue Note")));
        index(program().withMid().broadcasters(new Broadcaster("EO"))
                .relations(new Relation(eoLabel, null, "Evangelisch")));

        RelationFacet relationFacet = new RelationFacet();
        relationFacet.setName("test");
        MediaSearch search = new MediaSearch();
        search.setBroadcasters(TextMatcherList.must(TextMatcher.must("VPRO")));
        relationFacet.setFilter(search);
        MediaForm form = form().relationsFacet(relationFacet).build();

        MediaSearchResult result = getAndTestResult(form);

        final List<MultipleFacetsResult> relations = result.getFacets().getRelations();

        assertThat(relations).isNotEmpty();
        assertThat(relations.get(0).getName()).isEqualTo("test");
        assertThat(relations.get(0)).hasSize(1);
    }

    @Test
    public void testWithLocationFilter() {

        index(program()); // no locations
        final Location location1 = new Location("http://www.locations.nl/1", BROADCASTER);
        location1.setId(1L);
        index(program().locations(location1)); // just a location with
                                                       // no platform
        final Location location2 = new Location("http://www.locations.nl/2", BROADCASTER,
                Platform.INTERNETVOD);
        location2.setId(2L);
        index(program().authoritativeRecord(Platform.INTERNETVOD).locations(location2)); // a location with  a specific platform

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
    public void testListMembers() {

        Group group = index(group().mid("MID_0"));
        index(program().mid("MID_1")
            .memberOf(group, 0)
            .memberOf(group, 2)
        );
        index(program().mid("MID_2").memberOf(group, 1));

        MediaResult result = target.listMembers(group, null, Order.ASC, 0L, 10);

        assertThat(result).hasSize(3);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_2");
        assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_1");

    }



    @Test
    public void testListMembers3WithProfile() {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );
        Group group = index(group().mid("MID_0"));
        Group unrelatedGroup = index(group().mid("MID_100"));

        index(program().mid("MID_1").memberOf(group, 0).memberOf(group, 3).memberOf(unrelatedGroup, 3).broadcasters("BNN"));
        index(program().mid("MID_2").memberOf(group, 1));
        index(program().mid("MID_3").memberOf(group, 2).broadcasters("BNN"));


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
    public void testListMembersWithProfileAndOffet() {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );
        Group group = index(group().mid("MID_0"));
        index(program().mid("MID_1").memberOf(group, 0).memberOf(group, 3).broadcasters("BNN"));
        index(program().mid("MID_2").memberOf(group, 1));
        index(program().mid("MID_3").memberOf(group, 2).broadcasters("BNN"));


        MediaResult result = target.listMembers(group, omroepProfile, Order.ASC, 1L, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_3");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_1");

    }


    @Test
    public void testListEpisodes() {

        Group group = index(season().mid("MID_0"));
        index(broadcast().mid("MID_1").episodeOf(group, 0).episodeOf(group, 2));
        index(broadcast().mid("MID_2").episodeOf(group, 1));
        index(broadcast().mid("MID_3").episodeOf(MemberRef.builder().parent(group).number(3).added(LocalDate.of(2017, 7, 12).atStartOfDay(ZONE_ID).toInstant()).build()));
        index(broadcast().mid("MID_4").episodeOf(MemberRef.builder().parent(group).number(3).added(LocalDate.of(2017, 7, 11).atStartOfDay(ZONE_ID).toInstant()).build()));
        index(broadcast().mid("MID_5").episodeOf(group, 4));

        {
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

        { // now wit offset

            ProgramResult result = target.listEpisodes(group, null, Order.ASC, 3L, 10);
            assertThat(result).hasSize(3);
            assertThat(result.getTotal()).isEqualTo(6);
            assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_4");
            assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_3");
            assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_5");



        }

    }


    @Test
    public void testListEpisodesWithProfile() {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );

        Group group = index(season().mid("MID_0"));
        index(broadcast().mid("MID_1").episodeOf(group, 0).episodeOf("MID_0", 2).broadcasters("BNN"));
        index(broadcast().mid("MID_2").episodeOf(group, 1));
        index(broadcast().mid("MID_3").episodeOf(group, 3).broadcasters("BNN"));

        ProgramResult result = target.listEpisodes(group, omroepProfile, Order.ASC, 0L, 10);

        assertThat(result).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(2).getMid()).isEqualTo("MID_3");

    }


    @Test
    public void testListEpisodesWithProfileAndOffset() {

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
            new Filter(new BroadcasterConstraint("BNN"))
        );

        Group group = index(season().mid("MID_0"));
        index(broadcast().mid("MID_1")
            .episodeOf(group, 0)
            .episodeOf("MID_0", 2)
            .broadcasters("BNN")
        );
        index(broadcast().mid("MID_2")
            .episodeOf(group, 1)
        );
        index(broadcast().mid("MID_3")
            .episodeOf(group, 3)
            .broadcasters("BNN")
        );

        // group MID_0 contains episodes:
        // MID_1,MID_2,MID_1,MID_3
        // only MID_1 and MID3 are of BNN

        ProgramResult result = target.listEpisodes(group, omroepProfile, Order.ASC, 1L, 10);

        // so the list of offset 1 = MID_2, MID_1, MID3, but MID_2 is not on the profile. Hence the result should look like this:
        assertThat(result).hasSize(2);
        assertThat(result.getItems().get(0).getMid()).isEqualTo("MID_1");
        assertThat(result.getItems().get(1).getMid()).isEqualTo("MID_3");

    }

    @Test
    public void testRedirect() {
        Group group1 = index(group().published().mid("MID_0"));
        index(group().mergedTo(group1).mid("MID_1"));
        target.refillRedirectCache();
        Optional<String> result = target.redirect("MID_1");
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo("MID_0");
    }

    @Test
    public void testAgeRating() {
        index(program().mainTitle("t1").ageRating(_6));
        index(program().mainTitle("t2").ageRating(_12));
        index(program().mainTitle("t3").ageRating(ALL));

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

        assertEquals(2L, (long) target.find(null, form().ageRating(Pattern.compile("6|ALL")).build(), 0, null).getSize());


    }

    /**
     * We create an index with all the MediaObject and all their properties
     * to test that our mapping is complete.
     */
    @Test
    public void ableToIndexAllMediaObject(){
        assertThatCode(() -> {
            index(MediaTestDataBuilder.group().withEverything().published());
            index(MediaTestDataBuilder.segment().withEverything());
            index(MediaTestDataBuilder.program().withEverything().withSegmentsWithEveryting());
            index(MediaTestDataBuilder.group().withEverything().deleted());
            index(MediaTestDataBuilder.segment().withEverything().deleted());
            index(MediaTestDataBuilder.program().withEverything().withSegmentsWithEveryting().deleted());


        }).doesNotThrowAnyException();

    }

    @Test
    public void testGenreFilter() {
        index(program().mainTitle("t1").genres(new Genre("3.0.1.1.6")));

        {
            SearchResult<MediaObject> result = target.find(null, form().build(), 0, null);
            assertEquals(1, result.getSize().intValue());
            assertEquals("Jeugd - Amusement",
                    result.getItems().get(0).getResult().getGenres().first().getDisplayName());
            assertEquals("t1", result.getItems().get(0).getResult().getMainTitle());
        }

        index(program().mainTitle("t2").genres(new Genre("3.0.1.5")));

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
    public void testGenreFilterWildCard() {
        index(program().mainTitle("t1").genres(new Genre("3.0.1.1.6")));
        index(program().mainTitle("t2").genres(new Genre("3.0.1.5")));
        index(program().mainTitle("t3").genres(new Genre("3.0.1")));
        index(program().mainTitle("t4").genres(new Genre("3.0.2")));

        ProfileDefinition<MediaObject> genreProfile = new ProfileDefinition<>(
            new Filter(new GenreConstraint("3.0.1*")));
        {
            SearchResult<MediaObject> result = target.find(genreProfile, form().build(), 0, null);
            assertEquals(3, result.getSize().intValue());
        }
    }

    @Test
    public void testContentRatings() {
        index(program().mainTitle("t1").contentRatings(ANGST));
        index(program().mainTitle("t2").contentRatings(DRUGS_EN_ALCOHOL));
        index(program().mainTitle("t3").contentRatings(ANGST, DRUGS_EN_ALCOHOL));

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
    public void testAgeAndContentRatings() {
        index(program().mainTitle("t1").contentRatings(SEKS).ageRating(_16));

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
    public void testAgeRatingWithFacets() {
        index(program().mainTitle("t1").ageRating(_12));
        index(program().mainTitle("t2").ageRating(_12));
        index(program().mainTitle("t3").ageRating(_6));
        index(program().mainTitle("t4").ageRating(_6));
        index(program().mainTitle("t5").ageRating(_6));
        index(program().mainTitle("t6").ageRating(ALL));

        MediaSearchResult result = target.find(null,
            form()
                .ageRating(ALL, _6, _12)
                .ageRatingFacet(0)
                .build(), 0, null
        );
        List<TermFacetResultItem> ageRatings = result.getFacets().getAgeRatings();

        assertEquals((int) Arrays.stream(AgeRating.values()).filter(Displayable::display).count(), ageRatings.size());

        /* 3 x 6, 0 x 9, 2 * 12, 0 x 16, 1 * Alle Leeftijden */
        assertEquals(3, ageRatings.get(_6.ordinal()).getCount());
        assertEquals(_6.getXmlValue(), ageRatings.get(_6.ordinal()).getId());

        assertEquals(0, ageRatings.get(_9.ordinal()).getCount());
        assertEquals(_9.getXmlValue(), ageRatings.get(_9.ordinal()).getId());

        assertEquals(2, ageRatings.get(_12.ordinal()).getCount());
        assertEquals(_12.getXmlValue(), ageRatings.get(_12.ordinal()).getId());

        assertEquals(0, ageRatings.get(_16.ordinal()).getCount());
        assertEquals(_16.getXmlValue(), ageRatings.get(_16.ordinal()).getId());


        assertEquals(ALL.getXmlValue(), ageRatings.get(ALL.ordinal()).getId());
        assertEquals(ALL.getDisplayName(), ageRatings.get(ALL.ordinal()).getValue());


        {
            MediaSearchResult result1 = target.find(null, form().ageRating(_16).ageRatingFacet().build(), 0, null);
            assertEquals(1L, result1.getSelectedFacets().getAgeRatings().size());
            assertEquals(_16.getXmlValue(), result1.getSelectedFacets().getAgeRatings().get(0).getValue());
            assertEquals(0, result1.getSelectedFacets().getAgeRatings().get(0).getCount());
        }
    }

    @Test
    public void testContentRatingWithFacets() {
        index(program().mainTitle("t1").contentRatings(DISCRIMINATIE, DRUGS_EN_ALCOHOL, ANGST));
        index(program().mainTitle("t2").contentRatings(SEKS));
        index(program().mainTitle("t3").contentRatings(DRUGS_EN_ALCOHOL));
        index(program().mainTitle("t4").contentRatings(ANGST));
        index(program().mainTitle("t5").contentRatings(GEWELD, GROF_TAALGEBRUIK));
        index(program().mainTitle("t6").contentRatings(GROF_TAALGEBRUIK));

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
    public void testFindWithAgeRatingProfile() {
        index(program().mainTitle("sex!").contentRatings(SEKS).ageRating(_16));
        index(program().mainTitle("heel gewoon"));

        ProfileDefinition<MediaObject> omroepProfile = new ProfileDefinition<>(
                new Filter(new Or(new Not(new HasAgeRatingConstraint()), new AgeRatingConstraint(AgeRating.ALL),
                        new AgeRatingConstraint(AgeRating._9), new AgeRatingConstraint(AgeRating._6))));
        SearchResult<MediaObject> result = target.find(omroepProfile, null, 0, null);

        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void testFindWithContentRatingsProfile() {
        index(program().mainTitle("sex!").contentRatings(SEKS).ageRating(_16));
        index(program().mainTitle("heel gewoon"));

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
    public void testSortByTitles() {
        index(program()
            .mainTitle("aa")
            .lexicoTitle("bb")
            .mid("aa")
        );
        index(program()
            .mainTitle("bb")
            .lexicoTitle("aa")
            .mid("bb")
        );

        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder().textualType(MAIN).order(Order.ASC).build());

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
    public void testSortByLexico() {
        index(program()
            .mainTitle("bb")
            //.lexicoTitle("bb") Should be implicit
            .mid("bb")
        );
        index(program()
            .mainTitle("aa")
            .lexicoTitle("cc")
            .mid("aa")
        );
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
    public void testSortByLexicoForOwner() {
        index(program()
            .mainTitle("bbmis", OwnerType.MIS)      // so this is its npo lexico title
            .mainTitle("cc", BROADCASTER) // so this is its broadcaster lexico title
            .mid("MID1")
        );
        index(program()
            .mainTitle("ccmis", OwnerType.MIS)
            .mainTitle("bb", BROADCASTER)
            .lexicoTitle("ccnpolexico", OwnerType.NPO)      // so this is its npo lexico title
            .lexicoTitle("aa", OwnerType.MIS)      // so this is NOT its npo lexico title
            .lexicoTitle("bblexico", BROADCASTER) // so this is it's broadcaster lexico title
            .mid("MID2")
        );
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .ownerType(OwnerType.NPO)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID1");  // its npo lexicotitle is bbmis
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID2");  // its npo lexicotitle is ccnpolexico
        }
        {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .ownerType(BROADCASTER)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("MID2"); // its lexico title is bblexico
            assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("MID1"); // its lexico title is cc
        }
    }

    @Test
    public void testSortByLexicoForOwnerIllegalOwner() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MediaForm form = new MediaForm();
            form.addSortField(TitleSortOrder.builder()
                .textualType(TextualType.LEXICO)
                .ownerType(OwnerType.MIS)
                .order(Order.ASC)
                .build());

            SearchResult<MediaObject> result = target.find(null, form, 0, null);
            log.info("{}", result);
        });

    }

    @Test
    public void testFindByTitlesCaseSensitive() {
        index(program()
            .mainTitle("abcde", WHATS_ON) // no broadcaster title, so it should fall back to this.
            .mid("abcde")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("aaaaa")
            .mid("aa")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("bbbbb")
            .subTitle("aaa subtitle")
            .mid("bb")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );
        index(program()
            .mainTitle("AAAB")
            .subTitle("AAA subtitle")
            .mid("BB")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );

        MediaForm form = form()
            .titles(
                TitleSearch.builder()
                    .owner(BROADCASTER)
                    .type(MAIN)
                    .value("a*")
                    .matchType(WILDCARD)
                    .caseSensitive(true)
                    .build()
            )
            .asc(MediaSortField.creationDate)
            .build();


        SearchResult<MediaObject> result = target.find(null, form, 0, null);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("abcde");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("aa");
        log.info("{}", result);
    }

    @Test
    public void testFindByTitlesCaseInSensitive() {
        index(program()
            .mainTitle("abcde", WHATS_ON) // no broadcaster title, so it should fall back to this.
            .mid("abcde")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("aaaaa")
            .mid("aa")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("bbbbb")
            .subTitle("aaa subfuzzy")
            .mid("bb")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );
        index(program()
            .mainTitle("AAAB")
            .subTitle("AAA subtitle")
            .mid("BB")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );

        MediaForm form = form()
            .titles(
                TitleSearch.builder()
                    .owner(BROADCASTER)
                    .type(MAIN)
                    .value("a*")
                    .match(MUST)
                    .matchType(WILDCARD)
                    .caseSensitive(false)
                    .build()
            )
            .asc(MediaSortField.creationDate)
            .build();


        SearchResult<MediaObject> result = target.find(null, form, 0, null);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getItems().get(0).getResult().getMid()).isEqualTo("abcde");
        assertThat(result.getItems().get(1).getResult().getMid()).isEqualTo("aa");
        log.info("{}", result);
    }


    @Test
    public void testTitlesFacetsBackwards() {
        target.setScore(false);
        index(program()
            .mainTitle("abcde", WHATS_ON) // no broadcaster title, so it should fall back to this.
            .mid("abcde")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("aaaaa")
            .mid("aa")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("bbbbb")
            .subTitle("aaa subtitle")
            .mid("bb")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );

        MediaForm form = form()
            .asc(MediaSortField.creationDate)
            .build();
        form.setFacets(new MediaFacets());
        TitleFacetList list = new TitleFacetList();
        list.setSort(FacetOrder.COUNT_ASC);
        form.getFacets().setTitles(new TitleFacetList());

        JAXB.marshal(form, LoggerOutputStream.info(log));
        assertThat(form.getFacets().getTitles().asMediaFacet()).isTrue();

        MediaSearchResult result = target.find(null, form, 0, 0);
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getFacets().getTitles()).hasSize(4); // Actually, I think, it should have been 3, 'aaa subtitle' is not a main title
        log.info("{}", result);
    }



    @Test
    public void testTitlesFacetsWithTextualType() {
        target.setScore(false);
        index(program()
            .mainTitle("aaa")
            .subTitle("xxx")// no broadcaster title, so it should fall back to this.
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("yyy")
            .subTitle("bbb")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("yyy")
            .subTitle("aaa")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );

        MediaForm form = form()
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate))
            .build();
        form.setFacets(new MediaFacets());


        TitleFacet mainTitles;
        TitleFacet subTiltes;

        {
            TitleSearch subSearch1 = TitleSearch.builder()
                .value("a*")
                .match(MUST)
                .matchType(WILDCARD)
                .type(MAIN)
                .build();

            mainTitles  = new TitleFacet();
            mainTitles.setName("mainTitleswithA");
            mainTitles.setSubSearch(subSearch1);
        }
        {
            TitleSearch subSearch2 = TitleSearch.builder()
                .value("b*")
                .match(MUST)
                .matchType(WILDCARD)
                .type(TextualType.SUB)
                .build();

            subTiltes = new TitleFacet();
            subTiltes.setName("subtitlesWithB");
            subTiltes.setSubSearch(subSearch2);
        }

        form.getFacets().setTitles(new TitleFacetList(Arrays.asList(mainTitles, subTiltes)));

        assertThat(form.getFacets().getTitles().asMediaFacet()).isFalse();

        MediaSearchResult result  = target.find(null, form, 0, 0);
        //Jackson2Mapper.getPrettyInstance().writeValue(System.out, form);
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getFacets().getTitles()).hasSize(2);
        assertThat(result.getFacets().getTitles().get(0).getId()).isEqualTo("mainTitleswithA");
        assertThat(result.getFacets().getTitles().get(0).getCount()).isEqualTo(1); // namely, aaa
        assertThat(result.getFacets().getTitles().get(1).getId()).isEqualTo("subtitlesWithB");
        assertThat(result.getFacets().getTitles().get(1).getCount()).isEqualTo(1); // namely, bbb
    }

    @Test
    public void testTitlesFacetsWithTextualTypeAndCaseSensitive() {
        index(program()
            .mainTitle("AAA")
            .subTitle("xxx")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("AAA")
            .subTitle("yyy")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("bbb")
            .subTitle("aaa")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );

        MediaForm form = form()
            .sortOrder(MediaSortOrder.asc(MediaSortField.creationDate))
            .build();
        form.setFacets(new MediaFacets());

        TitleFacet aMainTitlesCaseSensitive;
        TitleFacet aCaseInsensitive;
        TitleFacet aSubTitlesCaseInsensitive;

        {
            TitleSearch subSearch1 = TitleSearch.builder()
                .value("A*")
                .match(MUST)
                .caseSensitive(true)
                .matchType(WILDCARD)
                .type(MAIN)
                .build();

            aMainTitlesCaseSensitive = new TitleFacet();
            aMainTitlesCaseSensitive.setName("mainTitleswithCapitalA");
            aMainTitlesCaseSensitive.setSubSearch(subSearch1);
        }
        {
            TitleSearch subSearch2 = TitleSearch.builder()
                .value("A*")
                .match(MUST)
                .caseSensitive(false)
                .matchType(WILDCARD)
                .build();

            aCaseInsensitive = new TitleFacet();
            aCaseInsensitive.setName("allTitlesWithA");
            aCaseInsensitive.setSubSearch(subSearch2);
        }

        {
            TitleSearch subSearch3 = TitleSearch.builder()
                .value("A*")
                .match(MUST)
                .type(TextualType.SUB)
                .caseSensitive(false)
                .matchType(WILDCARD)
                .build();

            aSubTitlesCaseInsensitive = new TitleFacet();
            aSubTitlesCaseInsensitive.setName("allSubtitlesWithA");
            aSubTitlesCaseInsensitive.setSubSearch(subSearch3);
        }

        form.getFacets().setTitles(new TitleFacetList(Arrays.asList(aMainTitlesCaseSensitive, aCaseInsensitive, aSubTitlesCaseInsensitive)));

        MediaSearchResult result = target.find(null, form, 0, 0);

        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getFacets().getTitles()).hasSize(3);
        assertThat(result.getFacets().getTitles().get(0).getId()).isEqualTo("mainTitleswithCapitalA");
        assertThat(result.getFacets().getTitles().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getTitles().get(1).getId()).isEqualTo("allTitlesWithA");
        assertThat(result.getFacets().getTitles().get(1).getCount()).isEqualTo(3);
        assertThat(result.getFacets().getTitles().get(2).getId()).isEqualTo("allSubtitlesWithA");
        assertThat(result.getFacets().getTitles().get(2).getCount()).isEqualTo(1);
    }

    @Test
    public void expandedTitles() throws IOException {
        index(program()
            .mainTitle("christmas", WHATS_ON) // no broadcaster title, so it should fall back to this.
            .mid("POW_123")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("abcde", WHATS_ON) // no broadcaster title, so it should fall back to this.
            .mid("POW_234")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 0))
        );
        index(program()
            .mainTitle("aaaaa")
            .mid("POW_345")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 1))
        );
        index(program()
            .mainTitle("bbbbb")
            .mid("POW_456")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );
        index(program()
            .mainTitle("BBBB")
            .mid("POW_567")
            .creationDate(LocalDateTime.of(2017, 10, 11, 10, 2))
        );

        MediaForm form = Jackson2Mapper.getInstance().readValue("{\n" +
            "    \"facets\": {\n" +
            "        \"titles\": [\n" +
            "            {\n" +
            "                \"name\": \"a\",\n" +
            "                \"subSearch\": {\n" +
            "                    \"type\": \"MAIN\",\n" +
            "                    \"value\": \"b*\",\n" +
            "                    \"matchType\": \"WILDCARD\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"name\": \"b\",\n" +
            "                \"subSearch\": {\n" +
            "                    \"type\": \"MAIN\",\n" +
            "                    \"value\": \"a*\",\n" +
            "                    \"matchType\": \"WILDCARD\"\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}\n", MediaForm.class);

        MediaSearchResult result = target.find(null, form, 0, 0);

        assertThat(result.getFacets().getTitles().get(0).getId()).isEqualTo("a");
        assertThat(result.getFacets().getTitles().get(1).getId()).isEqualTo("b");
        assertThat(result.getFacets().getTitles().get(0).getCount()).isEqualTo(2);
        assertThat(result.getFacets().getTitles().get(1).getCount()).isEqualTo(2);


        form.setSearches(MediaSearch.builder()
            .title(TitleSearch.builder()
                .value("b*")
                .type(MAIN)
                .matchType(WILDCARD)
                .build())
            .build());

        MediaSearchResult resultWithSearch = target.find(null, form, 0, 0);
        assertThat(resultWithSearch.getFacets().getTitles().get(0).getId()).isEqualTo("a");
        assertThat(resultWithSearch.getFacets().getTitles().get(0).getCount()).isEqualTo(2);
        assertThat(resultWithSearch.getFacets().getTitles().get(0).isSelected()).isTrue();

        assertThat(resultWithSearch.getFacets().getTitles().get(1).getId()).isEqualTo("b");
        assertThat(resultWithSearch.getFacets().getTitles().get(1).getCount()).isEqualTo(0);
        assertThat(resultWithSearch.getFacets().getTitles()).hasSize(2);
        assertThat(resultWithSearch.getSelectedFacets().getTitles()).hasSize(1);
    }


    @Test
    // NPA-490
    public void reruns() {
        index(program()
            .mid("mid_1")
            .mainTitle("original on ned1")
            .scheduleEvent(ScheduleEvent.builder().channel(Channel.NED1).start(LocalDateTime.of(2019, 7, 30, 12, 0).atZone(ZONE_ID).toInstant()).rerun(false).build())
        );

        index(program()
            .mid("mid_2")
            .mainTitle("rerun on ned1")
            .scheduleEvent(ScheduleEvent.builder().channel(Channel.NED1).start(LocalDateTime.of(2019, 7, 30, 13, 0).atZone(ZONE_ID).toInstant()).rerun(true).build())
        );

        index(program()
            .mid("mid_3")
            .mainTitle("original on ned2")
            .scheduleEvent(ScheduleEvent.builder().channel(NED2).start(LocalDateTime.of(2019, 7, 30, 14, 0).atZone(ZONE_ID).toInstant()).rerun(false).build())
        );

        index(program()
            .mid("mid_4")
            .mainTitle("rerun on ned2")
            .scheduleEvent(ScheduleEvent.builder().channel(NED2).start(LocalDateTime.of(2019, 7, 30, 15, 0).atZone(ZONE_ID).toInstant()).rerun(true).build())
        );


        // now for the more convulated. The question is, should this match or not?

        // we do match now
        index(program()
            .mid("mid_5")
            .mainTitle("rereun on ned1 but original on ned2")
            .scheduleEvent(
                ScheduleEvent.builder()
                    .channel(Channel.NED1)
                    .start(LocalDateTime.of(2019, 7, 30, 15, 0).atZone(ZONE_ID).toInstant())
                    .rerun(true)
                    .build()
            )
            .scheduleEvent(
                ScheduleEvent.builder()
                    .channel(NED2)
                    .start(LocalDateTime.of(2019, 7, 30, 15, 0).atZone(ZONE_ID).toInstant())
                    .rerun(false).build()
            )
        );

         MediaForm form = MediaForm.builder()
             .scheduleEvents(
                 ScheduleEventSearch.builder()
                     .original()
                     .channel(Channel.NED1) // not necessary at the same time!
                     .build()
             )
             .build();

        MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
        log.info("{}", resultWithSearch);
        assertThat(resultWithSearch).hasSize(1); // mid_1 and _not_ mid_5

    }
    private void indexWithGeoLocations() {
        index(program()
            .mid("mid_geo_1")
            .mainTitle("according to broadcaster about amsterdam")
            .geoLocations(BROADCASTER, GeoLocation.subject(AMSTERDAM))
            .geoLocations(AUTHORITY, GeoLocation.subject(UTRECHT))
          );
        index(program()
            .mid("mid_geo_2")
            .mainTitle("according to broadcaster produced in  amsterdam")
            .geoLocations(BROADCASTER, GeoLocation.producedIn(AMSTERDAM))
        );
        index(program()
            .mid("mid_geo_3")
            .mainTitle("according to broadcaster about hilversum")
            .geoLocations(BROADCASTER, GeoLocation.subject(HILVERSUM))
        );
        index(program()
            .mid("mid_geo_4")
            .mainTitle("according to authority about utrecht")
            .geoLocations(AUTHORITY, GeoLocation.subject(UTRECHT))
            .geoLocations(BROADCASTER) // not according to broadcaster
        );
        index(program()
            .mid("mid_geo_5")
            .mainTitle("according to authority about amsterdam")
            .geoLocations(AUTHORITY, GeoLocation.subject(AMSTERDAM))
            .geoLocations(BROADCASTER) // not according to broadcaster
        );
    }

    @Test
    public void testFindByGeoLocation() {
        indexWithGeoLocations();
        {
            // Now find all objects that according to broadcaster are about amsterdam
            MediaForm form = MediaForm.builder()
                .geoLocation(GeoLocationSearch.builder()
                    .owner(BROADCASTER)
                    .gtaaUri(URI.create(AMSTERDAM.getUri()))
                    .role(SUBJECT)
                    .build())
                .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(1);  // mid_geo_1
            form.getSearches().getGeoLocations().get(0).setOwner(null);
            // if you don't specify owner, it will be implicit 'broadcaster' too.
            resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(1);  // mid_geo_1

        }
        {
            // Now find all objects that according to authority are about amsterdam
            MediaForm form = MediaForm.builder()
                .geoLocation(GeoLocationSearch.builder()
                    .owner(AUTHORITY)
                    .gtaaUri(URI.create(AMSTERDAM.getUri()))
                    .role(SUBJECT)
                    .build())
                .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(1);  // mid_geo_4
        }

        {
            // Now search by name
            MediaForm form = MediaForm.builder()
                .geoLocation(GeoLocationSearch.builder()
                    .value("amsterdam")
                    .caseSensitive(false)
                    .role(SUBJECT)
                    .build())
                .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(1);  // mid_geo_4
        }
        {
            // Now search by name case sensitive!
            MediaForm form = MediaForm.builder()
                .geoLocation(GeoLocationSearch.builder()
                    .value("amsterdam")
                    .caseSensitive(true)
                    .role(SUBJECT)
                    .build())
                .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(0);  // It's Amsterdam
        }
        {
            // Get all the values without role filter
            MediaForm form = MediaForm.builder()
                    .geoLocation(GeoLocationSearch.builder()
                            .value("amsterdam")
                            .caseSensitive(false)
                            .build())
                    .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(2);
        }
        {
            // Get all the values without role filter
            MediaForm form = MediaForm.builder()
                    .geoLocation(GeoLocationSearch.builder()
                            .value("amsterdam")
                            .caseSensitive(false)
                            .build())
                    .build();
            MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
            log.info("{}", resultWithSearch);
            assertThat(resultWithSearch).hasSize(2);
        }
    }

    @Test
    @Disabled("Not yet implemented")
    public void testFacetByGeoName() {
        indexWithGeoLocations();

        MediaForm form = MediaForm.builder()
            .geoLocationFacet()
            .build();
        form.getFacets().getGeoLocations().setFilter(MediaSearch.builder().geoLocations(
            Arrays.asList(
                GeoLocationSearch.builder().owner(BROADCASTER).build())
        ).build());
        MediaSearchResult resultWithSearch = target.find(null, form, 0, 10);
        List<GeoLocationFacetResultItem> facets = resultWithSearch.getFacets().getGeoLocations();
        assertThat(facets).isNotEmpty();

    }

    @SuppressWarnings("SameParameterValue")
    private void redirect(String from, String to) {
        target.redirects.put(from, to);
    }

    private <B extends MediaBuilder<B, T>, T extends MediaObject> T index(B builder)  {
        if (builder.mediaObject().isMerged()) {
            builder.workflow(Workflow.MERGED);
        } else if (Workflow.DELETES.contains(builder.getWorkflow())) {
            builder.workflow(Workflow.DELETED);
        } else {
            builder.workflow(Workflow.PUBLISHED);
        }
        T object = builder.build();
        indexMediaObject(object);
        for (MemberRef ref : object.getMemberOf()) {
            indexRef(object, ref, memberRef);
        }

        if (object instanceof Program) {
            for (MemberRef ref : ((Program) object).getEpisodeOf()) {
                indexRef(object, ref, episodeRef);
            }
        }
        return object;
    }

    private void indexMediaObject(MediaObject object) {
        try {
            byte[] bytes = Jackson2Mapper.getPublisherInstance().writeValueAsBytes(object);
            String indexName = indexHelpers.get(ApiMediaIndex.APIMEDIA).getIndexName();
            ObjectNode indexResponse = indexHelpers.get(ApiMediaIndex.APIMEDIA).index(object.getMid(), bytes);
            log.info("Indexed {} {} ({})", indexName, indexResponse.get(Constants.Fields.ID), object.getWorkflow());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        refresh();

    }

    private void indexRef(MediaObject child, MemberRef object,  StandaloneMemberRef.ObjectType objectType) {
        StandaloneMemberRef ref = StandaloneMemberRef.builder()
            .childRef(child.getMid())
            .memberRef(object)
            .objectType(objectType)
            .build();
        String indexName = indexHelpers.get(ApiRefsIndex.APIMEDIA_REFS).getIndexName();
        assertThat(object.getMidRef()).isNotEmpty();
        ObjectNode jsonNodes = indexHelpers.get(ApiRefsIndex.APIMEDIA_REFS).indexWithRouting(
            ref.getId().toString(),
            ref,
            object.getMidRef());

        log.info("Indexed {} {}", indexName, jsonNodes.get(Constants.Fields.ID));
        refresh();
    }

    protected MediaSearchResult getAndTestResult(MediaForm form) {
        return getAndTestResult(target, form);
    }

}
