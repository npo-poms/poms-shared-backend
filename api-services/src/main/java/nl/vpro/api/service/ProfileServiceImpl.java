package nl.vpro.api.service;

import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.search.Search;
import nl.vpro.api.service.search.fiterbuilder.DocumentSearchFilter;
import nl.vpro.api.util.SolrQueryBuilder;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
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
@Service("profileService")
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private Search search;

//    @Autowired
//    private SolrServer solrServer;

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);


    private Map<String, String> archiveCache = new HashMap<String, String>();

//    @Autowired
//    SolrQueryBuilder solrQueryBuilder;



    public ProfileServiceImpl() {
    }

    @Override
    public Profile getProfile(String name) throws ServerErrorException{
        for (Profile profile : Profile.values()) {
            if (profile.getName().equals(name)) {
                if (StringUtils.isNotBlank(profile.getArchiveName())) {
                    setArchive(profile);
                }
                return profile;
            }
        }
        return Profile.DEFAULT;
    }

    /**
     * Tries to set the id of a named (poms) archive.The serach service is used to retrieve the poms archive from
     * a specific backend.
     * @param profile
     * @throws ServerErrorException
     */
    private void setArchive(Profile profile) throws ServerErrorException {
        log.debug("setting archive for profile " + profile.getName());
        String archiveName = profile.getArchiveName();
        if (StringUtils.isNotBlank(archiveName)) {
            if (!archiveCache.containsKey(archiveName)) {
                synchronized (this) {
                    archiveCache.put(archiveName, search.findArchiveId(archiveName));
                }
            }
            profile.setArchiveUrn(archiveCache.get(archiveName));
        }
    }
}
