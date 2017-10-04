package nl.vpro.pages.domain.es;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiPagesIndex {

    public static String NAME = "apipages";

    public static String source() {
        return source(settings());
    }

    public static String settings() {
        return "es5/setting/apipages.json";
    }

    public static String source(String s) {
        try {
            StringWriter writer = new StringWriter();
            InputStream inputStream = ApiPagesIndex.class.getClassLoader().getResourceAsStream(s);
            if (inputStream == null) {
                throw new IllegalStateException("Could not find " + s);
            }
            IOUtils.copy(inputStream, writer, "utf-8");
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    public static Map<String, Supplier<String>> mappingsAsMap() {
        return Arrays.stream(PageESType.values()).collect(Collectors.toMap(Enum::name, (v) -> v::source));
    }
}
