package nl.vpro.api.rs;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @author Michiel Meeuwissen
 * @since 5.4
 */
@Slf4j
public class Iterate {


    public static void iterate(Iterator<?> i, JsonGenerator jg, HttpServletResponse response, String fieldName, String forString) throws IOException {
        response.setContentType(ContentType.APPLICATION_JSON.toString());
        jg.writeStartObject();
        jg.writeArrayFieldStart(fieldName);

        Instant start = Instant.now();
        long count = 0;
        long errors = 0;

        try {
            while (i.hasNext()) {
                try {
                    Object mo = i.next();
                    jg.writeObject(mo);
                    count++;
                    if (count % 5000 == 0) {
                        log.info("Streamed for {}, entries: {}, errors: {} (busy {})", forString, count, errors, Duration.between(start, Instant.now()));
                    }
                } catch (IOException ioe) {
                    log.warn(ioe.getClass().getName() + ":" + ioe.getMessage()); // e.g. Client Aborted
                    break;
                } catch (Throwable t) {
                    log.error(t.getClass().getName() + ":" + t.getMessage(), t);
                    errors++;
                    if (errors > 20) {
                        log.error("Too many errors. Not continuing.");
                        break;
                    }
                }
            }
        } finally {
            jg.writeEndArray();
            jg.writeEndObject();
            jg.flush();
        }
        Duration duration = Duration.between(start, Instant.now());
        log.info("Streamed for {} entries: {}, errors: {}. Took {}", forString, count, errors, duration);

    }
}
