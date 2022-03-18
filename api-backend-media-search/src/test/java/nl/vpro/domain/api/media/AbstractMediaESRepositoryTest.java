package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.Test;

import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;
import nl.vpro.media.domain.es.ApiMediaIndex;

import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Log4j2
public class AbstractMediaESRepositoryTest {

    AbstractESMediaRepository repository = new AbstractESMediaRepository(mock(HighLevelClientFactory.class)) {

        @Override
        public RedirectList redirects() {
            throw new UnsupportedOperationException();

        }

        @Override
        public boolean isScore() {
            return true;

        }
    };
    {
        repository.setIndexName(ApiMediaIndex.NAME);
    }

    @Test
    public void searchRequest() throws IOException {

        AbstractESMediaRepository.SearchRequestWrapper sr = repository.mediaSearchRequest(
            null,
            MediaForm.builder().withEverything().build(),
            QueryBuilders.boolQuery(),
            0, 0);
        log.info(sr.toString());

    }
}
