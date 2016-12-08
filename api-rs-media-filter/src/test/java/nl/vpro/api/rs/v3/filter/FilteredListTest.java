package nl.vpro.api.rs.v3.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class FilteredListTest {

    @BeforeClass
    public static void init() {
        MediaPropertiesFilters.instrument();
    }

    @Test
    public void contains() throws Exception {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = new FilteredList<>("title", list);
        assertThat(filtered.contains("b")).isFalse();

    }


    @Test
    public void containsEmpty() throws Exception {
        ApiMediaFilter.set("title:1");
        List<String> list = Arrays.asList();
        FilteredList<String> filtered = new FilteredList<>("title", list);
        assertThat(filtered.contains("b")).isFalse();

    }




    @Test
    public void implicitTitle() throws Exception {
        ApiMediaFilter.set("none");
        List<String> list = Arrays.asList("a", "b", "c");
        FilteredList<String> filtered = new FilteredList<>("title", list);
        assertThat(filtered.contains("b")).isFalse();
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered).hasSize(1);

    }

    @Test
    public void scheduleEvent() throws Exception {
        ApiMediaFilter.set("");
        List<String> list = Collections.nCopies(101, "a");
        FilteredList<String> filtered = new FilteredList<>("scheduleevent", list);
        assertThat(filtered.contains("a")).isTrue();
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
        FilteredList<String> filtered = new FilteredList<>("somethingelse", list);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered).hasSize(101);

    }
}
