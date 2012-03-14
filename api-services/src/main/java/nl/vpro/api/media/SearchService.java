package nl.vpro.api.media;

import nl.vpro.api.media.search.SearchResult;
import org.apache.solr.common.SolrDocumentList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Date: 12-3-12
 * Time: 15:45
 *
 * @author Ernst Bunders
 */
public interface SearchService {

    public SearchResult searchMediaWithAncestor(String ancestorUrn, String term, Integer offset, Integer max);

}
