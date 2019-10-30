package nl.vpro.api.rs.filter;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.Title;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public class FilteredSortedTitleSetTest {


    @BeforeClass
    public static void init() {
        MediaPropertiesFilters.instrument();
    }


    @Test
    public void testNoTextualType() {

        ApiMediaFilter.set("title:1");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(2);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
        assertThat(filtered.first().get()).isEqualTo("mis title");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }


    @Test
    public void testWithTextualType() {

        ApiMediaFilter.set("title:main:1");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
        assertThat(filtered.first().get()).isEqualTo("mis title");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }

    @Test
    public void testWithTextualTypes() {

        ApiMediaFilter.set("title:main|sub:1");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER),
                Title.shortTitle("short", OwnerType.BROADCASTER)

            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(2);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
        assertThat(filtered.first().get()).isEqualTo("mis title");
        assertThat(new ArrayList<>(filtered).get(1).get()).isEqualTo("subtitle");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }
    @Test
    public void testWithTextualTypesMerge() {

        ApiMediaFilter.set("title:main:1,title:sub:2");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER),
                Title.sub("subtitle2", OwnerType.MIS),
                Title.shortTitle("short", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(3);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
        assertThat(filtered.first().get()).isEqualTo("mis title");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }

    @Test
    public void testWithTextualTypesMergeOverride() {

        ApiMediaFilter.set("title:sub:2,title:1");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER),
                Title.sub("subtitle2", OwnerType.MIS),
                Title.sub("subtitle3", OwnerType.MIS),
                Title.shortTitle("short", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(4);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
        assertThat(filtered.first().get()).isEqualTo("mis title");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }


    @Test
    public void implicitTitle() {
        ApiMediaFilter.set("none");
        SortedSet<Title> list = new TreeSet<>(Arrays.asList(
            Title.main("b", OwnerType.CERES),
            Title.main("a"),
            Title.main("c", OwnerType.MIS),
            Title.shortTitle("a")
        ));
        FilteredSortedTitleSet filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.first().get()).isEqualTo("a");


    }



    @Test
    public void testWithTextualTypeWithoutLimitSingular() {

        ApiMediaFilter.set("title:main:");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER),
                Title.sub("subtitle2", OwnerType.MIS),
                Title.shortTitle("short", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
    }


    @Test
    public void testWithTextualTypeWithoutLimitPlural() {

        ApiMediaFilter.set("titles:main:");
        Set<Title> list = new TreeSet<>(
            Arrays.asList(
                Title.main("mis title", OwnerType.MIS),
                Title.main("whats'on title", OwnerType.WHATS_ON),
                Title.sub("subtitle", OwnerType.BROADCASTER),
                Title.sub("subtitle2", OwnerType.MIS),
                Title.shortTitle("short", OwnerType.BROADCASTER)
            ));
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(2);
        assertThat(filtered.contains(Title.main("mis title", OwnerType.MIS))).isTrue();
    }
}
