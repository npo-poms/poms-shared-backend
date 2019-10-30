package nl.vpro.api.rs.filter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class FilteredSortedSetTest {

    @BeforeAll
    public static void init() {
        // Not really needed for this, but otherwise...
        MediaPropertiesFilters.instrument();
    }

    @Test
    public void contains() {
        ApiMediaFilter.set("title:1");
        Set<String> list = new HashSet<>(Arrays.asList("b", "a"));
        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isTrue();
        assertThat(filtered.contains("b")).isFalse();
    }

    @Test
    public void containsFromBack() {
        ApiMediaFilter.set("title:-1");
        Set<String> list = new HashSet<>(Arrays.asList("b", "a"));
        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.contains("a")).isFalse();
        assertThat(filtered.contains("b")).isTrue();
    }


    @Test
    public void firstAndLast() {
        ApiMediaFilter.set("title:1");
        Set<String> list = new HashSet<>(Arrays.asList("b", "a", "c"));
        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("title", list);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.first()).isEqualTo("a");
        assertThat(filtered.last()).isEqualTo("a");
    }


    @Test
    public void subset() {
        ApiMediaFilter.set("title:1");
        Set<String> list = new HashSet<>(Arrays.asList("b", "a", "c", "d"));


        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("title", list);
        assertThat(filtered).hasSize(1);

        assertThat(filtered.headSet("b")).hasSize(1);
        assertThat(filtered.headSet("b").first()).isEqualTo("a");
        assertThat(filtered.headSet("b").last()).isEqualTo("a");

        assertThat(filtered.tailSet("b")).hasSize(1);
        assertThat(filtered.tailSet("b").first()).isEqualTo("b");
        assertThat(filtered.tailSet("b").last()).isEqualTo("b");

        assertThat(filtered.subSet("b", "c")).hasSize(1);
        assertThat(filtered.subSet("b", "c").first()).isEqualTo("b");
        assertThat(filtered.subSet("b", "c").last()).isEqualTo("b");


    }

    @Test
    public void containsEmpty() {
        ApiMediaFilter.set("title:1");
        Set<String> list = new HashSet<>();
        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("title", list);
        assertThat(filtered.contains("b")).isFalse();

    }


    @Test
    public void somethingElse() {
        ApiMediaFilter.set("");
        AtomicInteger i = new AtomicInteger();
        Set<String> list = Collections.nCopies(101, "a").stream().map(a -> a + i.incrementAndGet()).collect(Collectors.toSet());
        FilteredSortedSet<String> filtered = FilteredSortedSet.wrap("somethingelse", list);
        assertThat(filtered.contains("a1")).isTrue();
        assertThat(filtered).hasSize(101);

    }
}
