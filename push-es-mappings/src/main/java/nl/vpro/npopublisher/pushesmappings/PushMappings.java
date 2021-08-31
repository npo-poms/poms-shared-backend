package nl.vpro.npopublisher.pushesmappings;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.*;
import nl.vpro.elasticsearchclient.ClientElasticSearchFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiCueIndex;

import static nl.vpro.es.ApiPageQueryIndex.APIPAGEQUERIES;
import static nl.vpro.es.ApiQueryIndex.APIQUERIES;
import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;
import static nl.vpro.pages.domain.es.ApiPageUpdatesIndex.PAGEUPDATES;
import static nl.vpro.pages.domain.es.ApiPagesIndex.APIPAGES;


/**
 * Pushes the required mappings.
 * The indices are default configured for reindexation, so with a very long refresh rate.
 *
 * If this is run when the indices exist already, then the index is only changed, and the refresh will be set to the default (30s)
 *
 * Can be used with maven exec plugin.
 *
 *  mihxil@baleno:~/npo/poms-shared-backend/trunk/push-es-mappings$ mvn  -Dhost=localhost -Dcluster=asfasd
 *
 *
 * Test
 * :  mvn  -Dhost=localhost:9221 -Dcluster=poms-test
 * Dev
 * : mvn -Dhost=localhost:9215 -Dcluster=poms-dev
 *  Production
 *  mihxil@baleno:~/npo/poms-shared-backend/trunk/push-es-mappings$ mvn  -Dhost=localhost:9209 -Dcluster=poms-prod
 *
 *  You can also just run it from Intellij.
 *
 * @author Michiel Meeuwissen
 * @since 5.12
 */
@SuppressWarnings("FieldMayBeFinal")
@Slf4j
public class PushMappings implements Callable<Integer> {


    @Option(names = {"-h", "--host"}, description = "host for elasticsearch")
    private String host = "localhost";

    @Option(names = {"-u", "--user"})
    private String username = null;

    @Option(names = {"-p", "--password"})
    private String password = null;

    @Option(names = {"-c", "--cluster"})
    private String cluster  = null;

    @Option(names = {"-r", "--force-replicas"})
    private Integer forceReplicas  = null;

    @Option(names = {"-o", "--only"})
    private String only  = "^.*$";

    @Option(names = {"--experimental"})
    private boolean  experimental  = false;


    public static void main(String[] argv) {
        int exitCode = new CommandLine(new PushMappings()).setTrimQuotes(true).execute(argv);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try (ClientElasticSearchFactory factory = new ClientElasticSearchFactory()) {
            factory.setHosts(host);
            if (StringUtils.isNotBlank(cluster) && ! "NOTGIVEN".equals(cluster)) {
                factory.setClusterName(cluster);
                log.info("Cluster name {}", cluster);
            }
            factory.setBasicUser(username);
            factory.setBasicPassword(password);

            waitForHealth(factory);

            Pattern onlyPattern = Pattern.compile(only);

            for (ElasticSearchIndex elasticSearchIndex : getIndices()) {

                if (! onlyPattern.matcher(elasticSearchIndex.getIndexName()).matches()) {
                    log.info("Skipping {}", elasticSearchIndex.getIndexName());
                    continue;
                }
                if (! experimental) {
                    elasticSearchIndex = elasticSearchIndex.withoutExperimental();
                }
                createIndexIfNecessaryAndPushMappings(factory, elasticSearchIndex);
            }
        }
        return 0;
    }



    protected  void createIndexIfNecessaryAndPushMappings(ClientElasticSearchFactory factory, ElasticSearchIndex elasticSearchIndex ) {
        try {
            IndexHelper helper = IndexHelper.of(log, factory, elasticSearchIndex).build();

            boolean exists = helper.checkIndex();

            if (! exists) {
                helper.createIndex(CreateIndex.builder()
                    .useNumberPostfix(true)
                    .forReindex(true) // no replicas, very long refresh
                    .mappingsProcessor(elasticSearchIndex.getMappingsProcessor())
                    .build());
            } else {
                // the index existed already, so simply reput the settings
                // and prepare the index for actual usage
                log.info("{} : {}", elasticSearchIndex, helper.count());
                log.info("Reput mappings");
                helper.reputMappings();
                log.info("Reput settings");
                helper.reputSettings((settings) -> {
                    if (forceReplicas != null) {
                        ObjectNode index = settings.with("settings").with("index");
                        index.put("number_of_replicas", forceReplicas);
                    }
                });
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        }
    }

    protected List<ElasticSearchIndex> getIndices() {
        List<ElasticSearchIndex> desired = new ArrayList<>(Arrays.asList(
            APIMEDIA,
            APIMEDIA_REFS,
            APIPAGES,
            PAGEUPDATES,
            APIQUERIES,
            APIPAGEQUERIES
        ));
        desired.addAll(ApiCueIndex.getInstances());
        return desired;
    }


    protected void waitForHealth(ClientElasticSearchFactory factory) throws InterruptedException {
        while (true) {
            try {
                Request request  = new Request("GET", "/_cat/health");
                request.setOptions(request.getOptions()
                    .toBuilder()
                    .addHeader("accept", "application/json"));
                Response response = factory
                    .client(PushMappings.class)
                    .performRequest(request);
                ArrayNode health = Jackson2Mapper.getLenientInstance()
                    .readerFor(ArrayNode.class)
                    .readValue(response.getEntity().getContent());

                String status  = health.get(0).get("status").textValue();
                boolean serviceIsUp = "green".equals(status) || "yellow".equals(status);
                log.info("status {}", status);
                if (serviceIsUp) {
                    break;
                }
            } catch (Exception e){
                log.info(e.getMessage());
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }
}
