package nl.vpro.media.domain.es;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public class ApiCueIndex {

    public static String NAME = ApiMediaIndex.NAME;

    public static String TYPE = "cue";


    public static String typeSource() {
        return source(mapping());
    }
    public static String mapping() {
        return "es5/mapping/cue.json";
    }

    public static String settings() {
        return "es5/setting/apimedia.json";
    }


    public static String source() {
        return source(settings());
    }

    public static String source(String s) {
        return ApiMediaIndex.source(s);
    }


    public static Map<String, Supplier<String>> mappingsAsMap() {
        Map<String, Supplier<String>> result = new HashMap<>();
        result.put(TYPE, ApiCueIndex::typeSource);
        return result;
    }

}
