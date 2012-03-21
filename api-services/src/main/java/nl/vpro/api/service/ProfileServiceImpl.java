package nl.vpro.api.service;

import nl.vpro.api.service.search.MediaSearchQueryAND;
import nl.vpro.domain.media.search.MediaType;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 19-3-12
 * Time: 11:52
 *
 * @author Ernst Bunders
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private SolrServer solrServer;

    private Map<String, String> archiveCache = new HashMap<String, String>();

    @Autowired
    public ProfileServiceImpl(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    @Override
    public Profile getProfile(String name) {
        for (Profile profile : Profile.values()) {
            if (profile.getName().equals(name)) {
                setArchive(profile);
                return profile;
            }
        }
        return Profile.DEFAULT;
    }

    private void setArchive(Profile profile) {
        log.debug("setting archive for profile " + profile.getName());
        String archiveName = profile.getArchiveName();
        if (StringUtils.isNotBlank(archiveName)) {
            if (!archiveCache.containsKey(archiveName)) {
                synchronized (this) {
                    fetchArchiveUrn(archiveName);
                }
            }
            profile.setArchiveUrn(archiveCache.get(archiveName));
        }
    }

    private void fetchArchiveUrn(String name) {
        log.debug("Profile not found in cache. look up in solr");
        String queryString = new MediaSearchQueryAND()
            .addMediaType(MediaType.ARCHIVE)
            .setMainTitle(name)
            .createQueryString();
        SolrQuery query = new SolrQuery(queryString);

        try {
            QueryResponse response = solrServer.query(query);
            if (response.getResults().getNumFound() == 1) {
                String archiveUrn = (String) ((SolrDocument) response.getResults().get(0)).getFieldValue("urn");
                archiveCache.put(name, archiveUrn);
            } else {
                throw new RuntimeException("Can not find archive with name " + name + "in Sorl, no or too many results");
            }
        } catch (SolrServerException e) {
            throw new RuntimeException("Something went wrong connecting to Solr service.", e);
        }
    }

}
