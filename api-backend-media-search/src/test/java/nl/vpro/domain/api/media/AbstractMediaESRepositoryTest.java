package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.common.xcontent.XContentType;

import nl.vpro.domain.api.AbstractESRepositoryTest;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.MediaESType;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public class AbstractMediaESRepositoryTest extends AbstractESRepositoryTest {




    protected static void createIndex(String index) {
        indexName = index;
        try {
            CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices()
                .prepareCreate(indexName)
                .setSettings(ApiMediaIndex.source(), XContentType.JSON);
            for (MediaESType type : MediaESType.values()) {
                createIndexRequestBuilder.addMapping(type.name(), type.source(), XContentType.JSON);
            }
            createIndexRequestBuilder.execute()
                .actionGet();
        } catch (ResourceAlreadyExistsException e) {
            log.info("Index exists");
        }
        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }




}
