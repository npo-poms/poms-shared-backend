/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.service.querybuilder.MediaSearchQuery;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.domain.media.MediaObject;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jcouchdb.db.Database;
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

    @Value("${solr.suggest.min.occurrence}")
    private Integer suggestionsMinOccurrence;

    @Value("${solr.suggest.limit}")
    private Integer suggestionsLimit;

    private SolrServer solrServer;

    private ConversionService conversionService;

    private ProfileService profileService;

    private Database couchDbMedaiServer;

    @Autowired
    public MediaServiceImpl(SolrServer solrServer, ConversionService conversionService, ProfileService profileService, Database couchDbMedaiServer) {
        this.solrServer = solrServer;
        this.conversionService = conversionService;
        this.profileService = profileService;
        this.couchDbMedaiServer = couchDbMedaiServer;
    }

    public String get(String urn) {
        return urn;
    }

    @Override
    public MediaSearchResult search(String query, String profileName, Integer offset, Integer max) {
        Profile profile = profileService.getProfile(profileName);
        MediaSearchQuery filterQuery = profile.createSearchQuery();
        String filterQueryString = filterQuery.createQueryString();

        SolrQuery solrQuery = new SolrQuery(filterQueryString);
        solrQuery.setFields("*", "score");
        if (StringUtils.isNotBlank(filterQueryString)) {
            solrQuery.setFilterQueries(filterQueryString);
        }
        solrQuery.add("qf", "titleMain^4.0 descriptionMain^2.0");
        solrQuery.setQuery(query);

        Integer queryMaxRows = max != null && max < maxResult ? max : maxResult;
        solrQuery.setRows(queryMaxRows);

        if (offset != null && offset > 0) {
            solrQuery.setStart(offset);
        }

        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchResult.class);
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new MediaSearchResult();
    }

    @Override
    public MediaSearchSuggestions searchSuggestions(String query, String profileName) {
        Profile profile = profileService.getProfile(profileName);
        MediaSearchQuery filterQuery = profile.createSearchQuery();
        String filterQueryString = filterQuery.createQueryString();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        if (StringUtils.isNotBlank(filterQueryString)) {
            solrQuery.setFilterQueries(filterQueryString);
        }
        solrQuery.setFacet(true);
        solrQuery.setFacetLimit(suggestionsLimit);
        solrQuery.addFacetField("titleMain", "descriptionMain");
        solrQuery.setFacetPrefix(query);
        solrQuery.setFacetMinCount(suggestionsMinOccurrence);
        solrQuery.setFields("titleMain", "descriptionMain");
        solrQuery.setRows(0);

        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchSuggestions.class);
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new MediaSearchSuggestions();
    }

    @Override
    public MediaObject getById(String id) {
        
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public void setSuggestionsMinOccurrence(Integer suggestionsMinOccurrence) {
        this.suggestionsMinOccurrence = suggestionsMinOccurrence;
    }

    public void setSuggestionsLimit(Integer suggestionsLimit) {
        this.suggestionsLimit = suggestionsLimit;
    }
}
