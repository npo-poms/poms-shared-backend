package nl.vpro.pages.domain.es;

import java.util.Arrays;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public enum PageUpdateESType {
    pageupdate,
    deletedpageupdate("page")
    ;

    private final String source;

    PageUpdateESType(String s) {
        source = s;
    }

    PageUpdateESType() {
        source = name();
    }
    public String source() {
        return ApiPagesIndex.source("es5/mapping/" + source + ".json");
    }

    public static String[] toString(PageUpdateESType... types) {
        return Arrays.stream(types).map(Enum::name).toArray(String[]::new);

    }

}
