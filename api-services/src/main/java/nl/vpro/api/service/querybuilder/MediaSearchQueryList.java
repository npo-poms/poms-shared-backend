package nl.vpro.api.service.querybuilder;

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
public final class MediaSearchQueryList extends MediaSearchQuery<MediaSearchQueryList> {

    private static final Logger log = LoggerFactory.getLogger(MediaSearchQueryList.class);

    private List<MediaSearchQuery> mediaSearchQueries = new ArrayList<MediaSearchQuery>();

    public MediaSearchQueryList(BooleanOp booleanOp) {
        super(booleanOp);
    }

    public MediaSearchQueryList addQuery(MediaSearchQuery mediaSearchQuery) {
        mediaSearchQueries.add(mediaSearchQuery);
        return this;
    }

    @Override
    public String createQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();
        sb.grouping = mediaSearchQueries.size() > 1;
        
        for (MediaSearchQuery query : mediaSearchQueries) {
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
    protected MediaSearchQueryList getInstance() {
        return this;
    }

}
