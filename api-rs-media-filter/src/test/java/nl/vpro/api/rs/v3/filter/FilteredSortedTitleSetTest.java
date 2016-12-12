package nl.vpro.api.rs.v3.filter;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;

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
        assertThat(filtered.first().getTitle()).isEqualTo("mis title");
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
        assertThat(filtered.first().getTitle()).isEqualTo("mis title");
        assertThat(filtered.contains(Title.main("what'son title", OwnerType.WHATS_ON))).isFalse();

    }
}
