package nl.vpro.poms.es;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class Utils {

    public static String resourceToString(String name) {
        try {
            StringWriter writer = new StringWriter();
            InputStream inputStream = Utils.class.getResourceAsStream(name);
            if (inputStream == null) {
                throw new IllegalStateException("Could not find " + name);
            }
            IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


}
