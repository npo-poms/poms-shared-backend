package nl.vpro.api.service.searchqueryfactory;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.searchfilterbuilder.BooleanOp;
import nl.vpro.api.service.searchfilterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.searchfilterbuilder.SearchFilter;
import nl.vpro.api.service.searchfilterbuilder.SearchFilterList;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.andFilter;
import static org.elasticsearch.index.query.FilterBuilders.orFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * This class creates the search query for the ElasticSearch service with solr plugin.
 * Date: 15-5-12
 * Time: 11:38
 *
 * @author Ernst Bunders
 */
public class ElasticSearchSorlQueryFactory extends AbstractSolrQueryFactory {


    @Override
    public SolrQuery createSearchQuery(Profile profile, String term, Integer max, Integer offset) {
        return createDefaultLuceneQuery(profile, term, max, offset);
    }

    @Override
    public SolrQuery createSuggestQuery(Profile profile, String term, Integer minOccurrence, Integer limit) {
        QueryBuilder query = matchAllQuery();
        SolrQuery solrQuery = createBasicQuery(profile, query);
        setFacetFields(term, minOccurrence, limit, solrQuery);
        return solrQuery;
    }


    private SolrQuery createBasicQuery(Profile profile, QueryBuilder query) {
        SearchFilter filterQuery = profile.createFilterQuery();

        if (filterQuery != null) {
            FilterBuilder filter = createFilter(filterQuery);
            query = filteredQuery(query, filter);
        }

        SolrQuery solrQuery = solrQueryBuilder.build(query.toString().replaceAll(" ", ""));
        solrQuery.set("q.dsl", "true");

        return solrQuery;
    }


    public FilterBuilder createFilter(SearchFilter mediaSearchQuery) {
        FilterBuilder filterBuilder = null;
        if (mediaSearchQuery instanceof SearchFilterList) {
            filterBuilder = parseFilterList((SearchFilterList) mediaSearchQuery);
        }

        if (mediaSearchQuery instanceof DocumentSearchFilter) {
            filterBuilder = parseBooleanFilter((DocumentSearchFilter) mediaSearchQuery);
        }
        return filterBuilder;
    }


    private FilterBuilder parseFilterList(SearchFilterList queryList) {
        FilterBuilder result = null;

        List<FilterBuilder> filterBuilderList = new ArrayList<FilterBuilder>();
        for (SearchFilter childQuery : queryList.getMediaSearchQueries()) {
            if (childQuery instanceof SearchFilterList) {
                filterBuilderList.add(parseFilterList((SearchFilterList) childQuery));
            } else if (childQuery instanceof DocumentSearchFilter) {
                // If this queryList is of type and, we can just add all the term filters
                // of the boolean query, and not wrap it in it's own and filter
                if (queryList.getBooleanOp() == BooleanOp.AND) {
                    //add filters to current and filter
                    filterBuilderList.addAll(gatherTermFilters((DocumentSearchFilter) childQuery));
                } else {
                    //or: create new nested and filter
                    filterBuilderList.add(parseBooleanFilter((DocumentSearchFilter) childQuery));
                }
            }
        }

        if (queryList.getBooleanOp() == BooleanOp.AND) {
            result = andFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }
        if (queryList.getBooleanOp() == BooleanOp.OR) {
            result = orFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }

        return result;
    }

    private FilterBuilder parseBooleanFilter(DocumentSearchFilter query) {
        List<FilterBuilder> fbl = gatherTermFilters(query);
        if (fbl == null) return null;

        return fbl.size() > 1 ?
            andFilter(fbl.toArray(new FilterBuilder[fbl.size()]))
            : fbl.get(0);
    }

    private List<FilterBuilder> gatherTermFilters(DocumentSearchFilter query) {
        List<FilterBuilder> fbl = new ArrayList<FilterBuilder>();
        for (MediaType mediaType : query.getMediaTypes()) {
            fbl.add(termFilter("mediaType", mediaType.name()));
        }

        for (AvFileFormat fileFormat : query.getLocationFormats()) {
            fbl.add(termFilter("location_formats", fileFormat.name()));
        }

        for (AvType avType : query.getAvTypes()) {
            fbl.add(termFilter("avType", avType.name()));
        }

        for (String descendant : query.getDescendants()) {
            fbl.add(termFilter("descendantOf", descendant));
        }

        if (StringUtils.isNotBlank(query.getMainTitle())) {
            fbl.add(termFilter("titleMain", query.getMainTitle()));
        }

        if (StringUtils.isNotBlank(query.getDocumentType())) {
            fbl.add(termFilter("documentType", query.getDocumentType()));
        }
        if (fbl.size() == 0) {
            return null;
        }
        return fbl;
    }

}
