/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import nl.vpro.api.service.Profile;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;

import java.util.regex.Pattern;

/**
 * User: rico
 * Date: 05/12/2012
 */
public class SuggestionFacetBuilder extends TermsFacetBuilder {

    public SuggestionFacetBuilder(Profile profile, String name, String queryString, int limit) {
        super(name);
        fields(profile.getSearchFields().toArray(new String[profile.getSearchFields().size()]));
        size(limit);
        regex("^" + Pattern.quote(queryString) + ".*");
    }
}
