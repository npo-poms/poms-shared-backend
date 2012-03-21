package nl.vpro.api.service.search;

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
public final class MediaSearchQueryOR extends MediaSearchQuery<MediaSearchQueryOR> {

    private static final Logger log = LoggerFactory.getLogger(MediaSearchQueryOR.class);

    private List<MediaSearchQueryAND> mediaSearchQueryANDs = new ArrayList<MediaSearchQueryAND>();

    public MediaSearchQueryOR addAnd(MediaSearchQueryAND mediaSearchQueryAND) {
        mediaSearchQueryANDs.add(mediaSearchQueryAND);
        return this;
    }

    @Override
    public String createQueryString() {
        BooleanGroupingStringBuilder sb = BooleanGroupingStringBuilder.ORBuilder();
        for (MediaSearchQueryAND and : mediaSearchQueryANDs) {
            sb.append(and.createQueryString());
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
    protected MediaSearchQueryOR getInstance() {
        return this;
    }

}
