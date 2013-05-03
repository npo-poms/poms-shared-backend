package nl.vpro.api.service.search.filterbuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 20-3-12
 * Time: 16:02
 *
 * @author Ernst Bunders
 */
public final class SearchFilterList extends SearchFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SearchFilterList.class);

    private final List<SearchFilter> searchFilters = new ArrayList<SearchFilter>();

    public SearchFilterList(BooleanOp booleanOp) {
        super(booleanOp);
    }

    public SearchFilterList add(SearchFilter searchFilter) {
        searchFilters.add(searchFilter);
        return this;
    }

    public List<SearchFilter> getSearchFilters() {
        return searchFilters;
    }

    @Override
    public String createSolrQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();
        sb.grouping = searchFilters.size() > 1;

        for (SearchFilter query : searchFilters) {
            sb.append(query.createSolrQueryString());
        }
        sb.close();

        if (StringUtils.isNotBlank(queryString)) {
            sb.stringBuilder.append(queryString);
        }

        String s = sb.toString();
        LOG.debug("query:" + s);
        return s;
    }
}