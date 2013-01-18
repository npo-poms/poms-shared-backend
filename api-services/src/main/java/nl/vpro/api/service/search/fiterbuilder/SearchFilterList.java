package nl.vpro.api.service.search.fiterbuilder;

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

    public SearchFilterList addQuery(SearchFilter<?> mediaSearchQuery) {
        searchFilters.add(mediaSearchQuery);
        return this;
    }

    /**
     * 'Media' search queries?
     * @return
     */
    public List<SearchFilter> getMediaSearchQueries() {
        return searchFilters;
    }

    @Override
    public String createQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();
        sb.grouping = searchFilters.size() > 1;

        for (SearchFilter query : searchFilters) {
            sb.append(query.createQueryString());
        }
        sb.close();

        if (StringUtils.isNotBlank(queryString)) {
            sb.stringBuilder.append(queryString);
        }

        String s = sb.toString();
        LOG.debug("query:" + s);
        return s;
    }

    @Override
    protected SearchFilterList getInstance() {
        return this;
    }

    @Override
    public boolean apply(Object object) {
        switch(getBooleanOp()) {
            case AND:
                for (SearchFilter<?> filter : getMediaSearchQueries()) {
                    if (! filter.apply(object)) return false;
                }
                return true;
            case OR:
                for (SearchFilter<?> filter : getMediaSearchQueries()) {
                    if (filter.apply(object)) return true;
                }
                return false;
            default:
                return true;
        }
    }

}
