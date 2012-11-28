package nl.vpro.spring.converter;

import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItem;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ernst
 * Date: 10/2/12
 * Time: 2:59 PM
 */
public class ESSearchResponseToSearchResultConverter implements Converter<SearchResponseExtender, MediaSearchResult> {
    @Override
    public MediaSearchResult convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        final int start = responseExtender.start() == null ? 0 : responseExtender.start();

        MediaSearchResult mediaSearchResult = new MediaSearchResult(response.hits().totalHits(), start, response.hits().maxScore());
        MediaSearchResultItem item;
        for (SearchHit hit : response.getHits()) {
            item = new MediaSearchResultItem();
            item.setTitle(hit.field("title").getValue().toString());
            item.setUrn(hit.field("urn").getValue().toString());
            item.setScore(hit.score());
            if(hit.field("descriptionMain")!= null){
                item.setDescription(hit.field("descriptionMain").value().toString());
            }

            if (hit.field("broadcasters") != null) {
                item.setBroadcaster(toStrings(hit.field("broadcasters").values()));
            }


            mediaSearchResult.addMediaSearchResultItem(item);
        }

        return mediaSearchResult;
    }

    private List<String> toStrings(List<Object> lo) {
        List<String> ls = new ArrayList<String>();
        for (Object o : lo) {
            ls.add(o.toString());
        }
        return ls;
    }
}
