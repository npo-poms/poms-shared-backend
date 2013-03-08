/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.spring.converter;

import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.GenericSearchFacet;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.GenericSearchResultItem;
import nl.vpro.jackson.MediaMapper;
import org.codehaus.jackson.JsonNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;
import org.elasticsearch.search.highlight.HighlightField;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: rico
 * Date: 28/11/2012
 */
public class ESSearchResponseToGenericSearchResultConverter implements Converter<SearchResponseExtender, GenericSearchResult> {
    private static MediaMapper mapper = new MediaMapper();

    @Override
    public GenericSearchResult convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        final int start = responseExtender.start() == null ? 0 : responseExtender.start();

        GenericSearchResult genericSearchResult = new GenericSearchResult(response.hits().totalHits(), start, response.hits().maxScore());
        GenericSearchResultItem item;

        for (SearchHit hit : response.getHits()) {
            item = new GenericSearchResultItem();
            item.setScore(hit.score());
            item.setType(hit.type());

            try {
                JsonNode node = mapper.readTree(hit.sourceAsString());
                for (Map.Entry<String, HighlightField> entry : hit.getHighlightFields().entrySet()) {
                    Text[] texts = entry.getValue().getFragments();
                    List<String> fragments = new ArrayList<String>(texts.length);
                    for (Text text : texts) {
                        fragments.add(text.string());
                    }
                    item.addHighLight(entry.getValue().getName(), fragments);
                }
                item.setResult(node);
                genericSearchResult.addSearchResultItem(item);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (response.getFacets() != null) {
            for (Map.Entry<String, Facet> facets : response.getFacets().getFacets().entrySet()) {
                Facet facet = facets.getValue();
                if (facet instanceof InternalStringTermsFacet) {
                    for (InternalStringTermsFacet.StringEntry stringEntry : ((InternalStringTermsFacet) facet).entries()) {
                        GenericSearchFacet genericSearchFacet = new GenericSearchFacet(stringEntry.getTerm(), stringEntry.getCount());
                        genericSearchResult.addSearchFacetValue(facets.getKey(), genericSearchFacet);
                    }
                }
            }
        }


        return genericSearchResult;
    }
}
