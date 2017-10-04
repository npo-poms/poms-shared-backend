package nl.vpro.pages.domain.es;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
public class ApiPageUpdatesIndex {

    public static String NAME = "pageupdates";


    public static String source() {
        return ApiPagesIndex.source("es5/setting/apipages.json");
    }


    public static Map<String, Supplier<String>> mappingsAsMap() {
        return Arrays.stream(PageUpdateESType.values())
            .collect(Collectors.toMap(Enum::name, (v) -> v::source)
            );
    }

}
