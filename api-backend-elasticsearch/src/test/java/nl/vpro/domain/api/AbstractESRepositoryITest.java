package nl.vpro.domain.api;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.media.broadcaster.BroadcasterServiceLocator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/broadcasterService.xml",
    "classpath:/esclientfactory.xml"})
@Log4j2
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
@Testcontainers
public abstract class AbstractESRepositoryITest {



    @SuppressWarnings("resource")
    @Container
    public static GenericContainer<?> opensearch = new GenericContainer<>("ghcr.io/npo-poms/opensearch:opendistro")
        .withExposedPorts(9200);


    protected static final String NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd't'HHmmss").format(LocalDateTime.now());
    protected static final  Map<ElasticSearchIndex, IndexHelper> indexHelpers = new HashMap<>();
    protected static boolean firstRun = true;

    protected static HighLevelClientFactory staticClientFactory;


    @Inject
    protected HighLevelClientFactory clientFactory;


    @Inject
    protected BroadcasterService broadcasterService;

    @BeforeEach
    public void abstractSetup() throws Exception {
        if (staticClientFactory == null) {
            ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());
            staticClientFactory = clientFactory;
            log.info("Elastic search for integration tests: " + opensearch.getHost()+ ":" + opensearch.getMappedPort(9200));
            clientFactory.setHosts(new HttpHost( opensearch.getHost(), opensearch.getMappedPort(9200)));
            clientFactory.invalidate();

            log.info("Using ES hosts: {}", clientFactory.getLowLevelFactory().getHosts());
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
        indexHelpers.clear();
    }

    @AfterAll
    public static void shutdown() throws IOException {
        for (IndexHelper indexHelper : indexHelpers.values()) {
            indexHelper.deleteIndex();
        }
        indexHelpers.clear();
        refresh();
        firstRun = true;
    }

    public static String getIndexName() {
        if (indexHelpers.size() == 1) {
            return indexHelpers.values().iterator().next().getIndexName();
        } else {
            throw new IllegalStateException("Expected exactly one index, but found " + indexHelpers.keySet());
        }
    }

    public static String getIndexName(ElasticSearchIndex elasticSearchIndex) {
        return indexHelpers.get(elasticSearchIndex).getIndexName();
    }

    public static  RestHighLevelClient highLevelClient() {
        return staticClientFactory.highLevelClient("test");
    }

    protected static IndexHelper createIndexIfNecessary(ElasticSearchIndex abstractIndex)  {
        return createIndexIfNecessary(abstractIndex, "test-" + abstractIndex.getIndexName() + "-" + NOW);
    }

    protected static IndexHelper createIndexIfNecessary(ElasticSearchIndex abstractIndex, String indexName)  {
        if (! indexHelpers.containsKey(abstractIndex)) {
            log.info("Creating index {}: {}", abstractIndex, indexName);
            IndexHelper helper = IndexHelper.of(log, staticClientFactory, abstractIndex)
                .indexName(indexName)
                .build();

            indexHelpers.put(abstractIndex, helper);
            helper.waitForHealth();

            helper.createIndex(CreateIndex.FOR_TEST);

            refresh();
        }
        return indexHelpers.get(abstractIndex);
    }


    protected static void clearIndices() {
        refresh();
        for (IndexHelper indexHelper : indexHelpers.values()) {
            long cleared = indexHelper.clearIndex();
            log.info("Cleared {} objects from {}", cleared, indexHelper);
        }
        refresh();
        for (IndexHelper indexHelper : indexHelpers.values()) {
            long count = indexHelper.count();
            if (count > 0) {
                throw new IllegalStateException(String.format("The index for %s still contains %d objects", indexHelper, count));
            }
        }


    }

    @SneakyThrows
    protected static void refresh() {
        for (IndexHelper indexHelper : indexHelpers.values()) {
            log.debug("Refreshing {}", indexHelper.getIndexName());
            indexHelper.refresh();
        }
    }

}
