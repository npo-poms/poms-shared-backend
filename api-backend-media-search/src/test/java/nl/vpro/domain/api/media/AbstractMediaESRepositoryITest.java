package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.media.domain.es.*;

import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public abstract class AbstractMediaESRepositoryITest extends AbstractESRepositoryITest {

    protected static void createIndicesIfNecessary() {
        String indexName = createIndexIfNecessary(APIMEDIA);
        createIndexIfNecessary(APIMEDIA_REFS, indexName + ApiRefsIndex.POSTFIX);
        for (ApiCueIndex i : ApiCueIndex.getInstances()) {
            createIndexIfNecessary(i);
        }
    }


}
