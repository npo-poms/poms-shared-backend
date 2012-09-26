package nl.vpro.api.service.search.fiterbuilder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 20-3-12
 * Time: 16:02
 *
 * @author Ernst Bunders
 */
public final class SearchFilterList extends SearchFilter<SearchFilterList> {

    private static final Logger log = LoggerFactory.getLogger(SearchFilterList.class);

    private List<SearchFilter> mediaSearchQueries = new ArrayList<SearchFilter>();

    public SearchFilterList(BooleanOp booleanOp) {
        super(booleanOp);
    }

    public SearchFilterList addQuery(SearchFilter mediaSearchQuery) {
        mediaSearchQueries.add(mediaSearchQuery);
        return this;
    }

    public List<SearchFilter> getMediaSearchQueries() {
        return mediaSearchQueries;
    }

    @Override
    public String createQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();
        sb.grouping = mediaSearchQueries.size() > 1;

        for (SearchFilter query : mediaSearchQueries) {
            sb.append(query.createQueryString());
        }
        sb.close();

        if (StringUtils.isNotBlank(queryString)) {
            sb.stringBuilder.append(queryString);
        }

        String s = sb.toString();
        log.debug("query:" + s);
        return s;
    }

    @Override
    protected SearchFilterList getInstance() {
        return this;
    }

}
