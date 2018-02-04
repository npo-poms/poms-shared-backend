package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.media.domain.es.MediaESType;

import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class AbstractMediaESRepositoryTest {

    AbstractESMediaRepository repository = new AbstractESMediaRepository(mock(ESClientFactory.class)) {
        @Override
        protected String[] getRelevantTypes() {
            return MediaESType.mediaObjects();
        }
    };
    {
        repository.setIndexName("media");
    }

    @Test
    public void searchRequest() {
        BoolQueryBuilder filter = QueryBuilders.boolQuery();
        SearchRequest sr = repository.searchRequest(repository.getLoadTypes(), null,
            MediaForm.builder().withEverything().build(), null,
            filter, 0, 0);
        log.info(filter.toString());
        log.info(sr.toString());

    }
}
