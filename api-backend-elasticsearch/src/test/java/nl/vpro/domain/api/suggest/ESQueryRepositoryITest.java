package nl.vpro.domain.api.suggest;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.elasticsearch.ESClientFactory;

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
        IOUtils.copy(ESQueryRepositoryITest.class.getResourceAsStream("/es5/setting/apiqueries.json"), settings);

        ByteArrayOutputStream mapping = new ByteArrayOutputStream();
        IOUtils.copy(ESQueryRepositoryITest.class.getResourceAsStream("/es5/mapping/query.json"), mapping);
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
    public void index() {
        repository.index(new Query("lubach", "vpro"));

    }


}
