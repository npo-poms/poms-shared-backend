/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import java.util.List;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.SearchSuggestions;

/**
 * User: rico
 * Date: 29/11/2012
 */
public interface SiteService {

    GenericSearchResult search(String profileName, String queryString, Integer offset, Integer maxResult, List<String> constraints, List<String> facets, List<String> sortFields, String response);

    SearchSuggestions searchSuggestions(String profileName, String queryString, List<String> constraints);
}
