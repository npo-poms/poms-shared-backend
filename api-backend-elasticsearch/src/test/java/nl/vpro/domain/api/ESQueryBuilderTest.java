package nl.vpro.domain.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ESQueryBuilderTest {

    @Test
    public void testFilterStopWords() {
        assertThat(ESQueryBuilder.filterStopWords("de vogels in de bomen")).isEqualTo("vogels bomen");
        assertThat(ESQueryBuilder.filterStopWords("de het een")).isEqualTo("de het een");
    }

    @Test
    public void testSplit1() {
        assertThat(ESQueryBuilder.split("de vogels in de bomen")).containsExactly("de", "vogels", "in", "de", "bomen");
    }

    @Test
    public void testSplit2() {
        assertThat(ESQueryBuilder.split("\"de vogels\" in de bomen")).containsExactly("\"de vogels\"", "in", "de", "bomen");
    }

    @Test
    public void testSplit3() {
        assertThat(ESQueryBuilder.split("  \"de vogels\"  in  de bomen")).containsExactly("\"de vogels\"", "in", "de", "bomen");
    }

    @Test
    public void testSplit4() {
        assertThat(ESQueryBuilder.split("in \"de bomen\"")).containsExactly("in", "\"de bomen\"");
    }

    @Test
    public void testSplit5() {
        assertThat(ESQueryBuilder.split("  \"de vogels\"  in  \"de bomen\"")).containsExactly("\"de vogels\"", "in", "\"de bomen\"");
    }


}
