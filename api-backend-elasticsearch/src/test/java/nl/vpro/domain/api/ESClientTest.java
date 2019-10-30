package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
@Disabled
public class ESClientTest  {


    @Test
    public void test() throws UnknownHostException {
        Settings settings = Settings.builder()
            //.put("cluster.name", "elasticsearch")
            .build();
        TransportClient client = new PreBuiltTransportClient(settings)
            .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
        log.info("Built {}", client);
        log.info("" + client.connectedNodes());
        QueryBuilder builder = QueryBuilders.matchAllQuery();
        log.info("Total hits {}", client.prepareSearch("apimedia").setSource(new SearchSourceBuilder().size(0).query(builder)).get().getHits().getTotalHits());
    }

    @Test
    public void nodeTest() throws UnknownHostException {
        Settings settings = Settings.builder()
            .put("path.home", "/tmp")
            .put("node.master", false)
            .put("node.data", false)
            .build();
        Environment environment = new Environment(settings, Paths.get("/tmp"));
        Node node = new Node(environment);

        Client client = node.client();

        log.info("Built {}", client);
        QueryBuilder builder = QueryBuilders.matchAllQuery();
        log.info("Total hits {}", client.prepareSearch("apimedia").setSource(new SearchSourceBuilder().size(0).query(builder)).get().getHits().getTotalHits());
    }
}
