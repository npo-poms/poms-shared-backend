package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.IndexHelper;
import nl.vpro.elasticsearch.TransportClientFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/broadcasterService.xml", "classpath:/esclientfactory.xml"})
@Slf4j
public abstract class AbstractESRepositoryITest {

    protected static String indexName = null;

    protected static Client client;

    @Inject
    protected TransportClientFactory clientFactory;


    @Inject
    protected BroadcasterService broadcasterService;


    @Before
    public void abstractSetup() {

        if (client == null) {
            client = clientFactory.client("test");
        }
        log.info("Built {}", client);
        ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());

        when(broadcasterService.find(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return new Broadcaster(id, id + "display");

        });
    }

    @AfterClass
    public static void shutdown() throws ExecutionException, InterruptedException {
        if (indexName != null) {
            client.admin().indices().prepareDelete(indexName).execute().get();
        }
    }

    protected static void createIndexIfNecessary(String index, Supplier<String> settings, Map<String, Supplier<String>> mappings) throws InterruptedException, ExecutionException, IOException {
        if (indexName == null) {
            indexName = index;
            IndexHelper
                .builder()
                .log(log)
                .client((s) -> client)
                .indexName(indexName)
                .settings(settings)
                .mappings(mappings)
                .build()
                .createIndex();
        }
        refresh();
    }

    protected static void clearIndex() {
        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
        while (true) {
            long shouldDelete = 0;
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            for (SearchHit hit : client.prepareSearch(indexName).setQuery(QueryBuilders.matchAllQuery()).setSize(100).get().getHits()) {
                log.info("deleting {}/{}/{}", hit.getIndex(), hit.getType(), hit.getId());

                DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(hit.getIndex(), hit.getType(), hit.getId());
                SearchHitField routing = hit.getFields().get("_routing");
                if (routing != null) {
                    for (Object r : routing.getValues()) {
                        deleteRequestBuilder.setRouting(r.toString());
                    }
                }
                bulkRequestBuilder.add(deleteRequestBuilder);
                shouldDelete++;
            }
            if (shouldDelete > 0) {
                client.bulk(bulkRequestBuilder.request()).actionGet();
                client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
                log.info("Deleted {} ", shouldDelete);
            } else {
                break;
            }
        }

    }


    protected static void refresh() throws ExecutionException, InterruptedException {
        client.admin().indices().refresh(new RefreshRequest(indexName)).get();

    }


    protected static String getTypeName(MediaObject media) {
        boolean deletedType = false;

        switch (media.getWorkflow()) {
            case DELETED:
            case REVOKED:
            case MERGED:
                deletedType = true;
                break;
            default:
                break;
        }
        return (deletedType ? "deleted" : "") + media.getClass().getSimpleName().toLowerCase();
    }

}
