package nl.vpro.api.media.search;

import org.apache.solr.common.SolrDocumentList;
import org.springframework.core.convert.converter.Converter;

import java.util.Iterator;
import java.util.Map;

/**
 * Date: 13-3-12
 * Time: 18:10
 *
 * @author Ernst Bunders
 */
public class SolrDocumentListConverter implements Converter<SolrDocumentList, SearchResult>{
    @Override
    public SearchResult convert(SolrDocumentList sdl) {
        SearchResult searchResult = new SearchResult(sdl.getNumFound(), sdl.getStart(), sdl.getMaxScore());
        for (Iterator<? extends Map<String, Object>> it = sdl.iterator(); it.hasNext(); ) {
            searchResult.addDocument(it.next());
        }
        return searchResult;
    }
}
