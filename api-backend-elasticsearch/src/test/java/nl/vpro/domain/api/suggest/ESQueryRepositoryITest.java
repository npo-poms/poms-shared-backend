package nl.vpro.domain.api.suggest;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import nl.vpro.domain.api.AbstractESRepositoryContainerTest;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;
import nl.vpro.es.ApiQueryIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@ContextConfiguration
@Log4j2
public class ESQueryRepositoryITest extends AbstractESRepositoryContainerTest {


    @Inject
    HighLevelClientFactory factory = mock(HighLevelClientFactory.class);

    ESQueryRepository repository;


    @Override
    protected void firstRun() {
        createIndexIfNecessary(ApiQueryIndex.APIQUERIES);
    }
    @BeforeEach
    public void init() {
        repository = new ESQueryRepository(factory);
        repository.setIndexName(indexHelpers.get(ApiQueryIndex.APIQUERIES).getIndexName());
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
