/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.service.search.Search;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.GenericSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Controller
public class SiteRestServiceImpl implements SiteRestService {
    @Autowired
    @Qualifier("essearch")
    private Search search;

    @Override
    public GenericSearchResult search(String profileName, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, String response) {
        return null;
    }

    @Override
    public MediaSearchSuggestions searchSuggestions(String profileName, String queryString, List<String> constraints) {
        return null;
    }
}
