/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.spring.converter;

import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.GenericSearchResultItem;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

/**
 * User: rico
 * Date: 28/11/2012
 */
public class ESSearchResponseToGenericSearchResultConverter implements Converter<SearchResponseExtender, GenericSearchResult> {
    @Override
    public GenericSearchResult convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        final int start = responseExtender.start() == null ? 0 : responseExtender.start();

        GenericSearchResult genericSearchResult = new GenericSearchResult(response.hits().totalHits(), start, response.hits().maxScore());
        GenericSearchResultItem item;
        for (SearchHit hit : response.getHits()) {
            item = new GenericSearchResultItem();
            item.setScore(hit.score());
            // document type aka mapping : hit.getFields().get("_type").value();
            for (Map.Entry<String, SearchHitField> entry : hit.getFields().entrySet()) {
                item.addValue(entry.getKey(), entry.getValue().getValues());
            }
            genericSearchResult.addGenericSearchResultItem(item);
        }

        return genericSearchResult;
    }
}
