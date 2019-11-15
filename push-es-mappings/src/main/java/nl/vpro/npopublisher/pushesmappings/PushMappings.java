package nl.vpro.npopublisher.pushesmappings;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearchclient.ClientElasticSearchFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.media.domain.es.ApiCueIndex;

import static nl.vpro.es.ApiPageQueryIndex.APIPAGEQUERIES;
import static nl.vpro.es.ApiQueryIndex.APIQUERIES;
import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;
import static nl.vpro.pages.domain.es.ApiPageUpdatesIndex.PAGEUPDATES;
import static nl.vpro.pages.domain.es.ApiPagesIndex.APIPAGES;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class PushMappings {

    public static void main(String[] argv) {
        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        if (argv.length > 0) {
            factory.setUnicastHosts(argv[0]);
        } else {
            factory.setUnicastHosts("http://localhost:9200");
        }
        if (argv.length > 1) {
            factory.setClusterName(argv[1]);
            log.info("Cluster name {}", argv[1]);
        }
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
                try {
                    IndexHelper helper = IndexHelper.of(log, factory, elasticSearchIndex).build();
                    if (!helper.createIndexIfNotExists(CreateIndex.builder()
                        .useNumberPostfix(true)
                        .forReindex(true)
                        .build())
                    ) {
                        log.info("{} : {}", elasticSearchIndex, helper.count());
                        helper.reputSettings();
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
