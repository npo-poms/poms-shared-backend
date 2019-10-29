package nl.vpro.media.domain.es;


import java.util.*;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public abstract class ApiCueIndex extends AbstractIndex {

    protected static List<ApiCueIndex> instances = new ArrayList<>();

    public static List<ApiCueIndex> getInstance() {
        return Collections.unmodifiableList(instances);
    }
    @lombok.Getter
    private final Locale locale;


    protected ApiCueIndex(String name, Locale locale) {
        super(name,  "/es7/mapping/cue.json");
        this.locale = locale;
    }


    public static Optional<ApiCueIndex> forLanguage(Locale locale) {
        return instances.stream().filter(i -> i.locale.equals(locale)).findFirst();
    }

}
