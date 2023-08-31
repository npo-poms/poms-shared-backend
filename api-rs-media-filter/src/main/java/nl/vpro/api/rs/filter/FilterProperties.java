package nl.vpro.api.rs.filter;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public interface FilterProperties {

    Integer get(String option);

    /**
     * Whether lists must be evaluated 'from the back'. i.e. if e.g. max = 1, the _last_ one of the list will be returned.
     */
    boolean fromBack();

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
        return new FilterPropertiesImpl(1, option, false);
    }

    static FilterProperties one(Enum<?> option) {
        return one(option.name().toLowerCase());
    }

    FilterProperties ONE = one((String) null);
    FilterProperties NONE = new FilterPropertiesImpl(0, null, false);
    FilterProperties ALL = new FilterPropertiesImpl(Integer.MAX_VALUE, null, false);

    FilterProperties MAX_100_FROM_BACK = new FilterPropertiesImpl(100, null, true);

}


@Log4j2
@ToString
@EqualsAndHashCode
class FilterPropertiesImpl implements FilterProperties {

    @Getter
    final Integer max;

    @Getter
    final String option;

    final boolean fromBack;

    FilterPropertiesImpl(Integer max, String option, boolean fromBack) {
        this.max = max;
        this.option = option;
        this.fromBack = fromBack;
    }

    @Override
    public Integer get(String option) {
        return this.option == null || Objects.equals(this.option, option) ? max : 0;
    }

    public boolean fromBack() {
        return fromBack;
    }


}

@Log4j2
class Combined implements FilterProperties {
    private final Map<String, FilterProperties> map = new HashMap<>();

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
    public boolean fromBack() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getOptions() {
        Collection<FilterProperties> values = map.values();
        return values.stream().map(FilterProperties::getOption).toArray(String[]::new);
    }

    public void put(FilterProperties newFilter) {
        FilterProperties prev = map.put(newFilter.getOption(), newFilter);
        if (prev != null) {
            log.warn("Replaced {} -> {}", prev, newFilter);
        }
    }
}

