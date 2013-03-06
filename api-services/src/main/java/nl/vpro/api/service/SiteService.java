/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.SearchSuggestions;

import java.util.List;

/**
 * User: rico
 * Date: 29/11/2012
 */
public interface SiteService {

    GenericSearchResult view(String profileName, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields);

    GenericSearchResult search(String profileName, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields, String response);

    SearchSuggestions searchSuggestions(String profileName, String queryString, List<String> constraints);
}
