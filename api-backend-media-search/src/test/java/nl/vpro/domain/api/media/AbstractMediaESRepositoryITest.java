package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.elasticsearch.IndexHelper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.MediaESType;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public class AbstractMediaESRepositoryITest extends AbstractESRepositoryITest {

    protected static void createIndex(String index) throws InterruptedException, ExecutionException, IOException {
        indexName = index;
        IndexHelper
            .builder()
            .log(log)
            .client((s) -> client)
            .indexName(indexName)
            .settings(ApiMediaIndex.source())
            .mappings(MediaESType.mappingsAsMap())
            .build()
            .createIndex();

        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }




}
