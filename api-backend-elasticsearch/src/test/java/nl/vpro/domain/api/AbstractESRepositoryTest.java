package nl.vpro.domain.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nl.vpro.domain.api.media.RedirectList;
import nl.vpro.domain.api.media.Redirector;
import nl.vpro.elasticsearch.ElasticSearchIndex;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
class AbstractESRepositoryTest {
    Map<String, String> redirects = new HashMap<>();
    AbstractESRepository<String> repo = new AbstractESRepository<String>(logName -> null) {
        @Override
        protected ElasticSearchIndex getIndex(String id, Class<?> clazz) {
            throw new UnsupportedOperationException("Not tested here");
        }

        @Override
        protected Redirector getDirectsRepository() {
            return () -> new RedirectList(Instant.now(), Instant.now(), redirects);
        }
    };
    {
        redirects.put("source", "target");
        redirects.put("anothersource", "target");
        redirects.put("source1", "target_but_source");
        redirects.put("target_but_source", "ultimate_target");
    }

    @Test
    void redirect() {
        assertThat(repo.redirect("a")).isNotPresent();
        assertThat(repo.redirect("source")).contains("target");
        assertThat(repo.redirect("anothersource")).contains("target");
        assertThat(repo.redirect("source1")).contains("ultimate_target");
        assertThat(repo.redirect("target_but_source")).contains("ultimate_target");


    }
}
