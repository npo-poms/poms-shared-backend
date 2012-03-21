/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;

/**
 * User: rico
 * Date: 08/03/2012
 */
public interface MediaService {

    public String get(String urn);

    public MediaSearchResult search(String query, String profile, Integer offset, Integer max);

    public MediaSearchSuggestions searchSuggestions(String query, String profile);

}
