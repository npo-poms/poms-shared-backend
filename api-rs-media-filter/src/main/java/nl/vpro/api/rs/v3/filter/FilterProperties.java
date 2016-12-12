package nl.vpro.api.rs.v3.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public interface FilterProperties {

    Integer get(String extra);

    default Integer get() {
        return get(null);
    }

    default String[] getExtras() {
        return new String[0];
    }

    default String getExtra() {
        String[] extras = getExtras();
        if (extras == null || extras.length == 0) {
            return null;
        }
        return extras[0];
    }

    static FilterProperties one(String extra) {
        return new FilterPropertiesImpl(1, extra);
    }

    static FilterProperties one(Enum extra) {
        return one(extra.name().toLowerCase());
    }

    FilterProperties ONE = one((String) null);
    FilterProperties NONE = new FilterPropertiesImpl(0, null);
    FilterProperties ALL = new FilterPropertiesImpl(Integer.MAX_VALUE, null);
}


class FilterPropertiesImpl implements FilterProperties {
    final Integer max;

    final String extra;

    FilterPropertiesImpl(Integer max, String extra) {
        this.max = max;
        this.extra = extra;
    }

    @Override
    public Integer get(String extra) {
        return this.extra == null || Objects.equals(this.extra, extra) ? max : 0;
    }

    @Override
    public String getExtra() {
        return extra;

    }
}


class Combined implements FilterProperties {
    final Map<String, FilterProperties> map = new HashMap<>();

    public Combined(FilterProperties first) {
        map.put(first.getExtra(), first);
    }

    @Override
    public Integer get(String extra) {
        return map.getOrDefault(extra, FilterProperties.NONE).get(extra);
    }

    @Override
    public String[] getExtras() {
        Collection<FilterProperties> values = map.values();
        return values.stream().map(FilterProperties::getExtra).toArray(String[]::new);
    }

}

