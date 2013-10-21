/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.filterbuilder;

/**
 * User: rico
 * Date: 20/06/2013
 */
public class MediaHasLocationFilter extends PrefixFieldFilter {
    public MediaHasLocationFilter(String field, String prefix) {
        super(field, prefix);
    }

    @Override
    public String createSolrQueryString() {
        // As locations are not searchable in solr we return everything
        return ("titleMain:*");
    }
}
