package nl.vpro.pages.domain.es;

import java.util.Arrays;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public enum PageESType {
    page,
    deletedpage("page")
    ;

    private final String source;

    PageESType(String s) {
        source = s;
    }

    PageESType() {
        source = name();
    }
    public String source() {
        return ApiPagesIndex.source("es5/mapping/" + source + ".json");
    }

    public static String[] toString(PageESType... types) {
        return Arrays.stream(types).map(Enum::name).toArray(String[]::new);

    }

}
