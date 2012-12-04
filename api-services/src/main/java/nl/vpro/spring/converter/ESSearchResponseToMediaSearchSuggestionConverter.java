/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.spring.converter;

import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.MediaSearchSuggestion;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

/**
 * User: rico
 * Date: 03/12/2012
 */
public class ESSearchResponseToMediaSearchSuggestionConverter implements Converter<SearchResponseExtender, MediaSearchSuggestions> {
    @Override
    public MediaSearchSuggestions convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        MediaSearchSuggestions mediaSearchSuggestions = new MediaSearchSuggestions();
        if (response.getFacets() != null) {
            for (Map.Entry<String, Facet> facets : response.getFacets().getFacets().entrySet()) {
                Facet facet = facets.getValue();
                if (facet instanceof InternalStringTermsFacet) {
                    for (InternalStringTermsFacet.StringEntry stringEntry : ((InternalStringTermsFacet) facet).entries()) {
                        MediaSearchSuggestion mediaSearchSuggestion = new MediaSearchSuggestion();
                        mediaSearchSuggestion.setValue(stringEntry.getTerm());
                        mediaSearchSuggestion.setOccurrence(Long.valueOf(stringEntry.getCount()));
                        mediaSearchSuggestions.addSuggestion(mediaSearchSuggestion);
                    }
                }
            }
        }
        return mediaSearchSuggestions;
    }
}
