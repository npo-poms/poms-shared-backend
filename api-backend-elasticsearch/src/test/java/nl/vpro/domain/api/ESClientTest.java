package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
@Ignore
public class ESClientTest  {


    @Test
    public void test() throws UnknownHostException {
        Settings settings = Settings.builder()
            //.put("cluster.name", "elasticsearch")
            .build();
        TransportClient client = new PreBuiltTransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
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
        Node node = new Node(settings);

        Client client = node.client();

        log.info("Built {}", client);
        QueryBuilder builder = QueryBuilders.matchAllQuery();
        log.info("Total hits {}", client.prepareSearch("apimedia").setSource(new SearchSourceBuilder().size(0).query(builder)).get().getHits().getTotalHits());
    }
}
