package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.media.domain.es.ApiMediaIndex;

import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class AbstractMediaESRepositoryTest {

    AbstractESMediaRepository repository = new AbstractESMediaRepository(mock(ESClientFactory.class)) {

    };
    {
        repository.setIndexName(ApiMediaIndex.NAME);
    }

    @Test
    public void searchRequest() {

        SearchRequest sr = repository.mediaSearchRequest(
            null,
            MediaForm.builder().withEverything().build(),
            QueryBuilders.boolQuery(),
            0, 0);
        log.info(sr.toString());

    }
}
