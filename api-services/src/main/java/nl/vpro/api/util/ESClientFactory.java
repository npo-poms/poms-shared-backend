package nl.vpro.api.util;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: ernst
 * Date: 10/1/12
 * Time: 11:34 AM
 */
public class ESClientFactory {
    private List<UrlProvider> transportAddresses = Collections.emptyList();


    private String clusterName;

    /**
     * When set to true, other cluster nodes are detected automatically
     */
    private boolean sniffCluster = true;

    /**
     * Set to true to ignore cluster name validation of connected nodes
     */
    private boolean ignoreClusterName = false;

    /**
     * How often to sample / ping the nodes listed and connected
     */
    private Integer pingTimeoutInSeconds;

    /**
     * How often should the client check the given node?
     */
    private Integer pingIntervalInSeconds;


    public Client buildClient(){
        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder()
                .put("client.transport.ignore_cluster_name", ignoreClusterName)
                .put("client.transport.sniff", sniffCluster);

        if(StringUtils.isNotBlank(clusterName)) {
            builder.put("cluster.name", clusterName);
        }
        if(pingTimeoutInSeconds != null) {
            builder.put("client.transport.ping_timeout", pingTimeoutInSeconds + "s");
        }
        if(pingIntervalInSeconds!= null) {
            builder.put("client.transport.nodes_sampler_interval", pingIntervalInSeconds+ "s");
        }

        TransportClient transportClient = new TransportClient(builder.build());
        for (UrlProvider urlProvider : transportAddresses) {
            transportClient.addTransportAddress(new InetSocketTransportAddress(urlProvider.getHost(), urlProvider.getPort()));
        }

        return transportClient;

    }


    public List<UrlProvider> getTransportAddresses() {
        return transportAddresses;
    }

    public void setTransportAddresses(List<UrlProvider> transportAddresses) {
        this.transportAddresses = transportAddresses;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public boolean isSniffCluster() {
        return sniffCluster;
    }

    public void setSniffCluster(boolean sniffCluster) {
        this.sniffCluster = sniffCluster;
    }

    public boolean isIgnoreClusterName() {
        return ignoreClusterName;
    }

    public void setIgnoreClusterName(boolean ignoreClusterName) {
        this.ignoreClusterName = ignoreClusterName;
    }

    public Integer getPingTimeoutInSeconds() {
        return pingTimeoutInSeconds;
    }

    public void setPingTimeoutInSeconds(Integer pingTimeoutInSeconds) {
        this.pingTimeoutInSeconds = pingTimeoutInSeconds;
    }

    public Integer getPingIntervalInSeconds() {
        return pingIntervalInSeconds;
    }

    public void setPingIntervalInSeconds(Integer pingIntervalInSeconds) {
        this.pingIntervalInSeconds = pingIntervalInSeconds;
    }
}
