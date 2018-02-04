package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import nl.vpro.media.domain.es.MediaESType;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class AbstractMediaESRepositoryTest {

    AbstractESMediaRepository repository = new AbstractESMediaRepository(null) {
        @Override
        protected String[] getRelevantTypes() {
            return MediaESType.mediaObjects();
        }
    };

    @Test
    public void searchRequest() {
        BoolQueryBuilder filter = QueryBuilders.boolQuery();
        repository.searchRequest(null, null,
            MediaForm.builder().withEverything().build(), null,
            filter, 0, 0);
        log.info(filter.toString());

    }
}
