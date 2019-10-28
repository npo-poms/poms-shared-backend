package nl.vpro.domain.api.suggest;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.elasticsearch.ESClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@ContextConfiguration
@Slf4j
public class ESQueryRepositoryITest extends AbstractESRepositoryITest {


    @Inject
    ESClientFactory factory = mock(ESClientFactory.class);

    ESQueryRepository repository;


    @Override
    protected void firstRun() throws IOException {
        ByteArrayOutputStream settings = new ByteArrayOutputStream();
        IOUtils.copy(ESQueryRepositoryITest.class.getResourceAsStream("/es7/setting/apiqueries.json"), settings);

        ByteArrayOutputStream mapping = new ByteArrayOutputStream();
        IOUtils.copy(ESQueryRepositoryITest.class.getResourceAsStream("/es7/mapping/query.json"), mapping);
        Map<String, Supplier<String>> mappings = new HashMap<>();
        mappings.put("query", mapping::toString);
        createIndexIfNecessary("queries", settings::toString, mappings);
    }
    @Before
    public void init() {
        repository = new ESQueryRepository(factory);
        repository.setIndexName(indexName);
    }


    @Test
    public void indexSuggestAndClear() throws InterruptedException {
        repository.index(new Query("lubach", "vpro"));
        repository.index(new Query("luitjes", "eo"));
        repository.setTtl(Duration.ofSeconds(1));

        refresh();
        SuggestResult suggest = repository.suggest("lu", "vpro", null);
        assertThat(suggest.getSize()).isEqualTo(1);
        assertThat(suggest.getItems().get(0).getText()).isEqualTo("lubach");

        Thread.sleep(1010L);
        repository.cleanSuggestions();
        refresh();

        suggest = repository.suggest("lu", "vpro", null);
        assertThat(suggest.getSize()).isEqualTo(0);


    }


}
