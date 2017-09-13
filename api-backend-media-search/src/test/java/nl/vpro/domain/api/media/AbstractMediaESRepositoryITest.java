package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.media.domain.es.ApiMediaIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public class AbstractMediaESRepositoryITest extends AbstractESRepositoryITest {

    protected static String createIndexIfNecessary(String index) throws InterruptedException, ExecutionException, IOException {
        return createIndexIfNecessary(index, ApiMediaIndex::source, ApiMediaIndex.mappingsAsMap());
    }


}
