package nl.vpro.domain.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.IndexHelper;
import nl.vpro.elasticsearch.TransportClientFactory;
import nl.vpro.poms.es.AbstractIndex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/broadcasterService.xml", "classpath:/esclientfactory.xml"})
@Slf4j
public abstract class AbstractESRepositoryITest {

    protected static final String NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd't'HHmmss").format(LocalDateTime.now());
    protected static final  Map<AbstractIndex, String> indexNames = new HashMap<>();
    protected static boolean firstRun = true;

    protected static Client client;

    static {
        log.info("JAVAPORT " + System.getProperty("integ.java.port"));

    }
    @Rule
    public TestRule noElasticSearch = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
        }
    };

    @Inject
    protected TransportClientFactory clientFactory;


    @Inject
    protected BroadcasterService broadcasterService;


    @Before
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
        if (firstRun) {
            firstRun();
            firstRun = false;
        }
    }

    protected abstract void firstRun() throws Exception;

    @BeforeClass
    public static void staticSetup() {
        indexNames.clear();
    }

    @AfterClass
    public static void shutdown() throws ExecutionException, InterruptedException {
        for (String name : indexNames.values()) {
            client.admin().indices().prepareDelete(name).execute().get();
        }
    }

    protected static String createIndexIfNecessary(AbstractIndex abstractIndex)  {
        return createIndexIfNecessary(abstractIndex, "test-" + abstractIndex.getIndexName() + "-" + NOW);
    }

    protected static String createIndexIfNecessary(AbstractIndex abstractIndex, String indexName)  {
        if (! indexNames.containsKey(abstractIndex)) {
            try {
                NodesInfoResponse response = client.admin()
                    .cluster().nodesInfo(new NodesInfoRequest()).get();
                log.info("" + response.getNodesMap());
                IndexHelper
                    .builder()
                    .log(log)
                    .client((s) -> client)
                    .indexName(indexName)
                    .settings(abstractIndex.settings())
                    .mappings(abstractIndex.mappingsAsMap())
                    .build()
                    .createIndex();
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


    public static Supplier<String> sourceSupplier(String name) {
        return () -> {
            try {
                StringWriter writer = new StringWriter();
                InputStream inputStream = AbstractESRepositoryITest.class.getClassLoader().getResourceAsStream(name);
                if (inputStream == null) {
                    throw new IllegalStateException("Could not find " + name);
                }
                IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
                return writer.toString();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }


    public static Map<String, Supplier<String>> mappingSupplier(String... name) {
        Map<String, Supplier<String>> result  = new HashMap<>();
        for (String n :name) {
            String key = new File(n).getName();
            int lastIndexOfDot = key.lastIndexOf(".");
            key = key.substring(0, lastIndexOfDot);
            result.put(key, sourceSupplier(n));
        }
        return result;
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

    }

    @SneakyThrows
    protected static void refresh() {
        for (String indexName : indexNames.values()) {
            try {
                client.admin().indices().refresh(new RefreshRequest(indexName)).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        }

    }



}
