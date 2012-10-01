package nl.vpro.api.service;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Date: 19-3-12
 * Time: 11:50
 *
 * @author Ernst Bunders
 *
 * Tries to find a named profile. If that fales, return the default profile
 */
public interface ProfileService {
    public Profile getProfile(String name);
}
