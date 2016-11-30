package nl.vpro.api.rs.v3.filter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class FilteredListTest {
    @Test
    public void contains() throws Exception {
        ApiMediaFilter.set("test:1");
        List<String> list = Arrays.asList("a", "b");
        FilteredList<String> filtered = new FilteredList<>("test", list);
        assertThat(filtered.contains("b")).isFalse();

    }


    @Test
    public void containsEmpty() throws Exception {
        ApiMediaFilter.set("test:1");
        List<String> list = Arrays.asList();
        FilteredList<String> filtered = new FilteredList<>("test", list);
        assertThat(filtered.contains("b")).isFalse();

    }
}