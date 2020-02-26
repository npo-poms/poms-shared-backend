package nl.vpro.npopublisher.pushesmappings;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;

import com.fasterxml.jackson.databind.node.ArrayNode;

import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearch.ElasticSearchIndex;
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
 * @author Michiel Meeuwissen
 * @since 5.12
 */
@Slf4j
public class PushMappings {

    public static void main(String[] argv) throws InterruptedException {

        String host = "http://localhost:9200";
        if (argv.length > 0) {
            host = argv[0];
        }
        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();

        factory.setUnicastHosts(host);
        if (argv.length > 1) {
            factory.setClusterName(argv[1]);
            log.info("Cluster name {}", argv[1]);
        }

        boolean serviceIsUp = false;
        while (!serviceIsUp) {
            TimeUnit.SECONDS.sleep(2);
            try {
                Request request  = new Request("GET", "/_cat/health");
                request.setOptions(request.getOptions().toBuilder().addHeader("accept", "application/json"));
                Response response = factory.client(PushMappings.class).performRequest(request);
                ArrayNode health = Jackson2Mapper.getLenientInstance().readerFor(ArrayNode.class).readValue(response.getEntity().getContent());

                String status  = health.get(0).get("status").textValue();
                serviceIsUp = "green".equals(status) || "yellow".equals(status);
                log.info("status {}", status);
            } catch (Exception e){
                log.info(e.getMessage());
            }
        }

        Pattern only = Pattern.compile("^.*$");
        //Pattern only = Pattern.compile("^pageupdates.*$");
        try {

            List<ElasticSearchIndex> desired = new ArrayList<>(Arrays.asList(
                APIMEDIA,
                APIMEDIA_REFS,
                APIPAGES,
                PAGEUPDATES,
                APIQUERIES,
                APIPAGEQUERIES
            ));
            desired.addAll(ApiCueIndex.getInstances());

            for (ElasticSearchIndex elasticSearchIndex : desired) {
                if (! only.matcher(elasticSearchIndex.getIndexName()).matches()) {
                    log.info("Skipping {}", elasticSearchIndex.getIndexName());
                    continue;
                }
                try {
                    IndexHelper helper = IndexHelper.of(log, factory, elasticSearchIndex).build();

                    // Try to created the index if it didn't exist already

                    boolean created = helper.createIndexIfNotExists(CreateIndex.builder()
                        .useNumberPostfix(true)
                        .forReindex(true) // 1 shard only, very long refresh
                        .build());

                    if (! created) {
                        // the index existed already, so simply reput the settings
                        // and prepare the index for actual usage
                        log.info("{} : {}", elasticSearchIndex, helper.count());
                        helper.reputSettings(false);
                        helper.reputMappings();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());

                }
            }
        } finally {
            factory.shutdown();
        }

    }
}
