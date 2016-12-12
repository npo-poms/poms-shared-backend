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
    public void test1() {

        ApiMediaFilter.set("title:1");
        Set<Title> list = new TreeSet<>(Arrays.asList(Title.main("a", OwnerType.MIS), Title.main("b", OwnerType.WHATS_ON)));
        System.out.println(list);
        FilteredSortedTextualTypableSet<Title> filtered = FilteredSortedTitleSet.wrapTitles("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains(Title.main("a", OwnerType.MIS))).isTrue();
        assertThat(filtered.contains(Title.main("b", OwnerType.WHATS_ON))).isFalse();

    }
}
