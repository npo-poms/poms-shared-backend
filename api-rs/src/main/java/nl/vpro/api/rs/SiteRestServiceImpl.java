/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.service.SiteService;
import nl.vpro.api.transfer.SearchSuggestions;
import nl.vpro.api.transfer.GenericSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import java.util.List;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Controller
public class SiteRestServiceImpl implements SiteRestService {
    @Autowired
    SiteService siteService;

    @Override
    public GenericSearchResult view(@PathParam("profile") String profileName, @DefaultValue("0") Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields) {
        return siteService.view(profileName,offset,maxResult,constraints,facets,sortFields);

    }

    @Override
    public GenericSearchResult search(String profileName, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields, String response) {
        return siteService.search(profileName,queryString,offset,maxResult,constraints,facets,sortFields,response);
    }

    @Override
    public SearchSuggestions searchSuggestions(String profileName, String queryString, List<String> constraints) {
        return siteService.searchSuggestions(profileName,queryString,constraints);
    }
}
