package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
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
            .build();
        NodeClient client = new NodeClient(settings, new ThreadPool(settings));
        log.info("Built {}", client);
        QueryBuilder builder = QueryBuilders.matchAllQuery();
        log.info("Total hits {}", client.prepareSearch("apimedia").setSource(new SearchSourceBuilder().size(0).query(builder)).get().getHits().getTotalHits());
    }
}
