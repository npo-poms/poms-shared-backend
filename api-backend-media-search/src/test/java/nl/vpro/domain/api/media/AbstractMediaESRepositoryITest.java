package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.media.domain.es.ApiMediaIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public abstract class AbstractMediaESRepositoryITest extends AbstractESRepositoryITest {

    protected static String createIndexIfNecessary(String index) {
        return createIndexIfNecessary(ApiMediaIndex.INSTANCE);
    }


}
