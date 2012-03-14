package nl.vpro.api.media;

import com.sun.net.ssl.internal.ssl.SSLSocketImpl;
import nl.vpro.api.media.search.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Date: 12-3-12
 * Time: 15:48
 *
 * @author Ernst Bunders
 */
@Service("searchService")
public class SearchServiceImpl implements SearchService{

    public static String SOLR_INDEX_NAME = "poms";
    private static final Logger log = LoggerFactory.getLogger(SSLSocketImpl.class);

    @Value("${solr.max.result}")
    private int maxResult;

    private SolrServer solrServer;

    private ConversionService conversionService;

    @Autowired
    public SearchServiceImpl(SolrServer solrServer, ConversionService conversionService) {
        this.solrServer = solrServer;
        this.conversionService = conversionService;
    }

    @Override
    //descendantOf:urn:vpro:media:group:11443747
    public SearchResult searchMediaWithAncestor(String ancestorUrn, String term, Integer offset, Integer max) {
        String query = "descendantOf:" + ancestorUrn;
        if (StringUtils.isNotBlank(term)) {
            query =  query + " " + term;
        }
        SolrQuery solrQuery = new SolrQuery(query);

        configureQuery(offset, max, solrQuery);
        try {
            QueryResponse response = solrServer.query(solrQuery);
            SearchResult searchResult = conversionService.convert(response.getResults(), SearchResult.class);
            return  searchResult;
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new SearchResult();
    }

    private void configureQuery(Integer offset, Integer max, SolrQuery solrQuery) {
        if (offset != null && offset > -1) {
            solrQuery.setStart(offset);
        }
        if (max != null && max > -1 && max < maxResult) {
            solrQuery.setRows(max);
        }else {
            solrQuery.setRows(maxResult);
        }
        solrQuery.setHighlight(true);
    }


}
