/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.service.search.MediaSearchQuery;
import nl.vpro.api.transfer.SearchResult;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

/**
 * User: rico
 * Date: 08/03/2012
 */
@Service("mediaService")
public class MediaServiceImpl implements MediaService {

    public static String MEDIA_CORE_NAME = "poms";
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);


    @Value("${solr.max.result}")
    private int maxResult;

    private SolrServer solrServer;

    private ConversionService conversionService;

    private ProfileService profileService;

    @Autowired
    public MediaServiceImpl(SolrServer solrServer, ConversionService conversionService, ProfileService profileService) {
        this.solrServer = solrServer;
        this.conversionService = conversionService;
        this.profileService = profileService;
    }

    public String get(String urn) {
        return urn;
    }

    @Override
    public SearchResult search(String query, String profileName, Integer offset, Integer max) {
        Profile profile = profileService.getProfile(profileName);
        MediaSearchQuery mediaSearchQuery = profile.createSearchQuery();
        mediaSearchQuery.setQueryString(query);

        SolrQuery solrQuery = new SolrQuery(mediaSearchQuery.createQueryString());

        Integer queryMaxRows = max != null && max < maxResult ? max : maxResult;
        solrQuery.setRows(queryMaxRows);

        if (offset != null && offset > 0) {
            solrQuery.setStart(offset);
        }

        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response.getResults(), SearchResult.class);
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new SearchResult();
    }


    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }
}
