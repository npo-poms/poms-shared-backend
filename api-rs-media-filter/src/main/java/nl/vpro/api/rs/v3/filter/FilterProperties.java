package nl.vpro.api.rs.v3.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public interface FilterProperties {

    Integer get(String option);

    default Integer get() {
        return get(null);
    }

    default String[] getOptions() {
        return new String[0];
    }

    default String getOption() {
        String[] options = getOptions();
        if (options == null || options.length == 0) {
            return null;
        }
        return options[0];
    }

    static FilterProperties one(String option) {
        return new FilterPropertiesImpl(1, option);
    }

    static FilterProperties one(Enum option) {
        return one(option.name().toLowerCase());
    }

    FilterProperties ONE = one((String) null);
    FilterProperties NONE = new FilterPropertiesImpl(0, null);
    FilterProperties ALL = new FilterPropertiesImpl(Integer.MAX_VALUE, null);
}


class FilterPropertiesImpl implements FilterProperties {
    final Integer max;

    final String option;

    FilterPropertiesImpl(Integer max, String extra) {
        this.max = max;
        this.option = extra;
    }

    @Override
    public Integer get(String option) {
        return this.option == null || Objects.equals(this.option, option) ? max : 0;
    }

    @Override
    public String getOption() {
        return option;

    }
}


class Combined implements FilterProperties {
    final Map<String, FilterProperties> map = new HashMap<>();

    public Combined(FilterProperties first) {
        map.put(first.getOption(), first);
    }

    @Override
    public Integer get(String option) {
        return map.getOrDefault(option,
            map.getOrDefault(null, FilterProperties.NONE)
        ).get(option);
    }

    @Override
    public String[] getOptions() {
        Collection<FilterProperties> values = map.values();
        return values.stream().map(FilterProperties::getOption).toArray(String[]::new);
    }

}

