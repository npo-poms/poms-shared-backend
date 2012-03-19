package nl.vpro.spring.converter;

import nl.vpro.api.transfer.SearchResult;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Date: 13-3-12
 * Time: 18:10
 *
 * @author Ernst Bunders
 */
public class SolrDocumentListConverter implements Converter<SolrDocumentList, SearchResult> {
    @Override
    public SearchResult convert(SolrDocumentList sdl) {
        SearchResult searchResult = new SearchResult(sdl.getNumFound(), sdl.getStart(), sdl.getMaxScore());
        Map<String, Object> document;

        for (Iterator<SolrDocument> it = sdl.iterator(); it.hasNext(); ) {
            SolrDocument solrDocument = it.next();
            document = new HashMap<String, Object>();
            for (String fieldName : solrDocument.getFieldNames()) {
                document.put(fieldName, solrDocument.getFieldValue(fieldName));
            }
            searchResult.addDocument(document);
        }
        return searchResult;
    }
}
