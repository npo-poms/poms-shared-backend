/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.spring.converter;

import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.SearchSuggestion;
import nl.vpro.api.transfer.SearchSuggestions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

/**
 * User: rico
 * Date: 03/12/2012
 */
public class ESSearchResponseToSearchSuggestionConverter implements Converter<SearchResponseExtender, SearchSuggestions> {
    @Override
    public SearchSuggestions convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        SearchSuggestions searchSuggestions = new SearchSuggestions();
        if (response.getFacets() != null) {
            for (Map.Entry<String, Facet> facets : response.getFacets().getFacets().entrySet()) {
                Facet facet = facets.getValue();
                if (facet instanceof InternalStringTermsFacet) {
                    for (InternalStringTermsFacet.StringEntry stringEntry : ((InternalStringTermsFacet) facet).entries()) {
                        SearchSuggestion searchSuggestion = new SearchSuggestion(stringEntry.getTerm(),Long.valueOf(stringEntry.getCount()));
                        searchSuggestions.addSuggestion(searchSuggestion);
                    }
                }
            }
        }
        return searchSuggestions;
    }
}
