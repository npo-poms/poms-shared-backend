package nl.vpro.api.rs.filter;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class FilteredListTest {

    static {
        MediaPropertiesFilters.instrument();
    }

    @BeforeAll
    public static void init() {

    }

    @Test
    public void contains() {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered.contains("b")).isFalse();
    }

    @Test
    public void containsNegativeTruncate() {
        ApiMediaFilter.set("title:-1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isFalse();
        assertThat(filtered.contains("b")).isTrue();

    }


    @Test
    public void containsEmpty() {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList();
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered.contains("b")).isFalse();
    }





    @Test
    public void scheduleEvent() {
        ApiMediaFilter.set("");
        List<String> list = new ArrayList<>();
        list.addAll(Collections.nCopies(50, "a"));
        list.addAll(Collections.nCopies(51, "b"));
        List<String> expected = new ArrayList<>();
        expected.addAll(Collections.nCopies(49, "a"));
        expected.addAll(Collections.nCopies(51, "b"));

        FilteredList<String> filtered = FilteredList.wrap("scheduleevent", list);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered.contains("b")).isTrue();
        assertThat(filtered).containsExactlyElementsOf(expected);

        assertThat(filtered).hasSize(100);

        ApiMediaFilter.removeFilter();

        assertThat(filtered).hasSize(101);

        ApiMediaFilter.set("all");

        assertThat(filtered).hasSize(101);


    }

    @Test
    public void somethingElse() {
        ApiMediaFilter.set("");
        List<String> list = Collections.nCopies(101, "a");
        FilteredList<String> filtered = FilteredList.wrap("somethingelse", list);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered).hasSize(101);

    }
}
