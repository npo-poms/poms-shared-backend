package nl.vpro.api.rs.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class FilteredListTest {

    @BeforeClass
    public static void init() {
        // Not really needed for this, but otherwise...
        MediaPropertiesFilters.instrument();
    }

    @Test
    public void contains() throws Exception {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered.contains("b")).isFalse();
    }

    @Test
    public void containsNegativeTruncate() throws Exception {
        ApiMediaFilter.set("title:-1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isFalse();
        assertThat(filtered.contains("b")).isTrue();

    }


    @Test
    public void containsEmpty() throws Exception {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList();
        FilteredList<String> filtered = FilteredList.wrap("title", list);
        assertThat(filtered.contains("b")).isFalse();

    }





    @Test
    public void scheduleEvent() throws Exception {
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
    public void somethingElse() throws Exception {
        ApiMediaFilter.set("");
        List<String> list = Collections.nCopies(101, "a");
        FilteredList<String> filtered = FilteredList.wrap("somethingelse", list);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered).hasSize(101);

    }
}
