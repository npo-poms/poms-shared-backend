package nl.vpro.npopublisher.pushesmappings;

import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.*;
import nl.vpro.elasticsearchclient.ClientElasticSearchFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.logging.simple.Log4j2SimpleLogger;
import nl.vpro.media.domain.es.ApiCueIndex;

import static nl.vpro.elasticsearch.Constants.P_INDEX;
import static nl.vpro.elasticsearch.Constants.P_SETTINGS;
import static nl.vpro.elasticsearch.Status.yellow;
import static nl.vpro.es.ApiPageQueryIndex.APIPAGEQUERIES;
import static nl.vpro.es.ApiQueryIndex.APIQUERIES;
import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;
import static nl.vpro.pages.domain.es.ApiPageUpdatesIndex.PAGEUPDATES;
import static nl.vpro.pages.domain.es.ApiPagesIndex.APIPAGES;


/**
 * Pushes the required mappings.
 * The indices are default configured for reindexation, so with a very long refresh rate.
 * <p>
 * If this is run when the indices exist already, then the index is only changed, and the refresh will be set to the default (30s)
 * <p>
 * Can be used with maven exec plugin.
 * <p>
 *  mihxil@baleno:~/npo/poms-shared-backend/trunk/push-es-mappings$ mvn  -Dhost=localhost -Dcluster=asfasd
 * <p>
 *
 * Test
 * :  mvn  -Dhost=https://vpc-poms-plus-elasticsearch-test-tu2husqhnj43tmsqnj2fvpi2hy.eu-central-1.es.amazonaws.com:9240/ -Dcluster=755036500103:poms-plus-elasticsearch-test -Duser=elasticsearch -Dpassword=`cat ~/npo/poms-forward/es.test.password`
 *
 *    mvn  -Dhost=https://vpc-poms-plus-elasticsearch-acc-bk5ui3ubahkwhmygv7h7nqbti4.eu-central-1.es.amazonaws.com:9230/ -Dcluster=755036500103:poms-plus-elasticsearch-acc -Duser=elasticsearch -Dpassword=`cat ~/npo/poms-forward/es.acc.password`
 * Dev
 * : mvn -Dhost=localhost:9215 -Dcluster=poms-dev -Dexperimental=true
 *  Production
 *  mihxil@baleno:~/npo/poms-shared-backend/trunk/push-es-mappings$ mvn  -Dhost=localhost:9209 -Dcluster=poms-prod
 * <p>
 *  You can also just run it from Intellij.
 *
 * @author Michiel Meeuwissen
 * @since 5.12
 */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@Log4j2
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

    @Option(names = {"--distribution"})
    private Distribution  distribution  = null;


    static void main(String[] argv) {
        int exitCode = new CommandLine(new PushMappings()).setTrimQuotes(true).execute(argv);
        log.info("Ready with exit code {}", exitCode);
    }

    @Override
    public Integer call() {
        try (ClientElasticSearchFactory factory = new ClientElasticSearchFactory()) {
            factory.setHosts(host);
            if (StringUtils.isNotBlank(cluster) && ! "NOTGIVEN".equals(cluster)) {
                factory.setClusterName(cluster);
                log.info("Cluster name {}", cluster);
            }
            factory.setBasicUser(username);
            File file = new File(password);
            if (file.canRead()) {
                password = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            }
            factory.setBasicPassword(password);

            IndexHelper.waitForHealth(
                factory.client(PushMappings.class),
                Log4j2SimpleLogger.of(log),
                Duration.ofSeconds(10),
                yellow
            );

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
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        return 0;
    }



    protected  void createIndexIfNecessaryAndPushMappings(ClientElasticSearchFactory factory, ElasticSearchIndex elasticSearchIndex ) {
        try (
            IndexHelper helper = IndexHelper.of(log, factory, elasticSearchIndex)
                .distribution(distribution)
                .build()) {


            boolean exists = helper.checkIndex();

            if (! exists) {
                helper.createIndex(CreateIndex.builder()
                    .useNumberPostfix(true)
                    .forReindex(true) // no replicas, very long refresh
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
                        ObjectNode index = settings.withObject(P_SETTINGS).withObject(P_INDEX);
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


}
