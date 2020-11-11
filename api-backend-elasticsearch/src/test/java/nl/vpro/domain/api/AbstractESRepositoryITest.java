package nl.vpro.domain.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;

import nl.vpro.media.broadcaster.BroadcasterServiceLocator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/broadcasterService.xml", "classpath:/esclientfactory.xml"})
@Slf4j
public abstract class AbstractESRepositoryITest {

    protected static final String NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd't'HHmmss").format(LocalDateTime.now());
    protected static final  Map<ElasticSearchIndex, String> indexNames = new HashMap<>();
    protected static boolean firstRun = true;

    protected static RestHighLevelClient client;

    static {
        log.info("JAVAPORT " + System.getProperty("integ.java.port"));

    }

    @Inject
    protected HighLevelClientFactory clientFactory;


    @Inject
    protected BroadcasterService broadcasterService;


    @BeforeEach
    public void abstractSetup() throws Exception {
        if (client == null) {
            client = clientFactory.client("test");
            log.info("Built {}", client);
            ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());
        }

        when(broadcasterService.find(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return new Broadcaster(id, id + "display");

        });
        BroadcasterServiceLocator.setInstance(broadcasterService);
        if (firstRun) {
            log.info("First run of {}", this);
            firstRun();
            firstRun = false;
        }
    }

    protected abstract void firstRun() throws Exception;

    @BeforeAll
    public static void staticSetup() {
        indexNames.clear();
    }

    @AfterAll
    public static void shutdown() throws ExecutionException, InterruptedException {
        for (String name : indexNames.values()) {
            client.admin().indices().prepareDelete(name).execute().get();
        }
        indexNames.clear();
        refresh();
        firstRun = true;
    }

    public static String getIndexName() {
        if (indexNames.size() == 1) {
            return indexNames.values().iterator().next();
        } else {
            throw new IllegalStateException("Expected exactly one index, but found " + indexNames);
        }
    }

    protected static String createIndexIfNecessary(ElasticSearchIndex abstractIndex)  {
        return createIndexIfNecessary(abstractIndex, "test-" + abstractIndex.getIndexName() + "-" + NOW);
    }

    protected static String createIndexIfNecessary(ElasticSearchIndex abstractIndex, String indexName)  {
        if (! indexNames.containsKey(abstractIndex)) {
            log.info("Creating index {}: {}", abstractIndex, indexName);
            try {
                NodesInfoResponse response = client.admin()
                    .cluster().nodesInfo(new NodesInfoRequest()).get();
                log.info("" + response.getNodesMap());
                IndexHelper.of(log,
                    (s) -> client, abstractIndex)
                    .indexName(indexName)
                    .build()
                    .createIndexIfNotExists(CreateIndex.FOR_TEST);
                indexNames.put(abstractIndex, indexName);
            } catch (NoNodeAvailableException noNodeAvailableException) {
                log.warn("No elastic search node could be found with {}", client);
                log.info("Please start up local elasticsearch");
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }

            refresh();
        }
        return indexNames.get(abstractIndex);
    }





    protected static void clearIndices() {
        for (String indexName : indexNames.values()) {
            clearIndex(indexName);
        }


    }
    protected static void clearIndex(String indexName) {
        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
        while (true) {
            long shouldDelete = 0;
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            for (SearchHit hit : client.prepareSearch(indexName)
                .setQuery(QueryBuilders.matchAllQuery()).setSize(100).get().getHits()) {
                log.info("deleting {}/{}", hit.getIndex(), hit.getId());

                DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(hit.getIndex(), hit.getType(), hit.getId());
                DocumentField routing = hit.getFields().get("_routing");
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
        log.info("Cleared {}", indexName);
        refresh();

    }

    @SneakyThrows
    protected static void refresh() {
        for (String indexName : indexNames.values()) {
            try {
                log.info("Refreshing {}", indexName);
                client.admin().indices().refresh(new RefreshRequest(indexName)).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        }

    }



}
