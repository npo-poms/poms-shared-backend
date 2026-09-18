package nl.vpro.api.rs.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.Title;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
@SuppressWarnings("deprecation")
class FilteredSortedTitleSetTest {

    static {
        MediaPropertiesFilters.instrument();
    }


    @Test
    void noTextualType() {

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
    void withTextualType() {

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
    void withTextualTypes() {

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
    void withTextualTypesMerge() {

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
    void withTextualTypesMergeOverride() {

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
    void implicitTitle() {
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
    void withTextualTypeWithoutLimitSingular() {

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
    void withTextualTypeWithoutLimitPlural() {

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
