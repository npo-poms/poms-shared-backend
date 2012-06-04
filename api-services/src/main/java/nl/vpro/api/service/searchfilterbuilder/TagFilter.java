package nl.vpro.api.service.searchfilterbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 4-6-12
 * Time: 14:31
 *
 * @author Ernst Bunders
 */
public class TagFilter {
    private List<String> tags = new ArrayList<String>();
    private BooleanOp booleanOp;

    public TagFilter(BooleanOp booleanOp) {
        this.booleanOp = booleanOp;
    }

    public TagFilter addTag(String tag) {
        tags.add(tag);
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }

    public BooleanOp getBooleanOp() {
        return booleanOp;
    }
}
