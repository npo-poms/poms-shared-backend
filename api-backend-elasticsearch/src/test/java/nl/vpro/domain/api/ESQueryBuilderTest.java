package nl.vpro.domain.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ESQueryBuilderTest {

    @Test
    void filterStopWords() {
        assertThat(ESQueryBuilder.filterStopWords("de vogels in de bomen")).isEqualTo("vogels bomen");
        assertThat(ESQueryBuilder.filterStopWords("de het een")).isEqualTo("de het een");
    }

    @Test
    void split1() {
        assertThat(ESQueryBuilder.split("de vogels in de bomen")).containsExactly("de", "vogels", "in", "de", "bomen");
    }

    @Test
    void split2() {
        assertThat(ESQueryBuilder.split("\"de vogels\" in de bomen")).containsExactly("\"de vogels\"", "in", "de", "bomen");
    }

    @Test
    void split3() {
        assertThat(ESQueryBuilder.split("  \"de vogels\"  in  de bomen")).containsExactly("\"de vogels\"", "in", "de", "bomen");
    }

    @Test
    void split4() {
        assertThat(ESQueryBuilder.split("in \"de bomen\"")).containsExactly("in", "\"de bomen\"");
    }

    @Test
    void split5() {
        assertThat(ESQueryBuilder.split("  \"de vogels\"  in  \"de bomen\"")).containsExactly("\"de vogels\"", "in", "\"de bomen\"");
    }


}
