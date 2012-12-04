/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.service.search.Search;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.elasticsearch.search.facet.FacetBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User: rico
 * Date: 29/11/2012
 */
@Service("siteService")
public class SiteServiceImpl implements SiteService {
    @Autowired
    @Qualifier("essearch")
    private Search search;

    @Autowired
    private ProfileService profileService;

    @Value("${solr.max.result}")
    private int globalMaxResult;

    @Value("${solr.suggest.min.occurrence}")
    private Integer suggestionsMinOccurrence;

    @Value("${solr.suggest.limit}")
    private Integer suggestionsLimit;

    @Override
    public GenericSearchResult search(String profileName, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields, String response) {
        Profile profile=profileService.getProfile(profileName);
        Integer queryMaxRows = maxResult != null && maxResult < globalMaxResult ? maxResult : globalMaxResult;

        GenericSearchResult result=search.search(profile,queryString,offset,queryMaxRows,constraints,facets,sortFields);
        // reduce SearchResult according to repsonse specification
        return result;
    }

    @Override
    public MediaSearchSuggestions searchSuggestions(String profileName, String queryString, List<String> constraints) {
        Profile profile=profileService.getProfile(profileName);
        MediaSearchSuggestions suggestions=search.suggest(profile, queryString,constraints, suggestionsMinOccurrence, suggestionsLimit);
        return suggestions;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
