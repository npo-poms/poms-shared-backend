package nl.vpro.api.rs;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.poms.shared.ExtraHeaders;
import nl.vpro.util.*;

/**
 * @author Michiel Meeuwissen
 * @since 5.4
 */
@Slf4j
public class Iterate {


    /**
     * @param creator  Supplies an iterator of objects, which will be used to make json objects.
     *                 The JsonGenerator argument can be ignored, but it can also be used to write opening json
     *                 and 'keepAlive' stuff.
     *
     * @param streamer When the iterator is created, this has to write it to the JsonGenerator
     * @param responseBuilderConsumer Optionally you can build the response further before it it returned. E.g. to add headers.
     */
    @SafeVarargs
    public static <T, E extends Exception> Response streamingJson(
        final ExceptionUtils.ThrowingFunction<JsonGenerator, CloseableIterator<T>, E> creator,
        final JsonConsumer<T> streamer,
        final Consumer<Response.ResponseBuilder>... responseBuilderConsumer) throws Exception {


        final PipedOutputStream pipedOutputStream = new PipedOutputStream();
        final PipedInputStream pipedInputStream = new PipedInputStream();
        pipedInputStream.connect(pipedOutputStream);

        final JsonGenerator jg = Jackson2Mapper.INSTANCE.getFactory()
            .createGenerator(pipedOutputStream, JsonEncoding.UTF8)
            ;

        final CloseableIterator<T> iterator;
        try {
            iterator = creator.applyWithException(jg);
        } catch (Exception e) {
            CloseableIterator.closeQuietly(jg);
            throw e;
        }

        final AtomicInteger closed = new AtomicInteger(0);
        final Runnable closeResources = () ->  {
            if (closed.getAndIncrement() == 0) {
                log.debug("Closing {}", iterator);
                CloseableIterator.closeQuietly(iterator);
            }  else {
                log.info("Closed already {}", iterator);
            }
        };


        final SecurityContext context = SecurityContextHolder.getContext();
        // ForkJoinPool.commonPool() doesn't work because serviceloader may not be available in classloader of tomcat?
        // see e.g. also https://github.com/talsma-ict/context-propagation/issues/94
        final Future<?> submit = ThreadPools.copyExecutor.submit(() -> {
            SecurityContextHolder.setContext(context);
            try {
                streamer.accept(iterator, jg);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                closeResources.run();
            }
            SecurityContextHolder.clearContext();
            log.debug("Ready");
        });

        final StreamingOutput streamingOutput = output -> {
            boolean ready = false;
            try (InputStream buffered = new BufferedInputStream(pipedInputStream, IOUtils.DEFAULT_BUFFER_SIZE)) {
                // used BufferedInputStream will cause that the buffer used in copyLarge needs not be entirely every time (see java.io.BufferedInputStream.read(byte[], int, int) (may fix

                byte[] buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                long copied = 0;
                int n;
                // doing it ourselves, so we can flush also output
                while (IOUtils.EOF != (n = buffered.read(buffer))) {
                    output.write(buffer, 0, n);
                    output.flush();
                    copied += n;
                }
                log.debug("Streamed {} bytes", copied);
                ready = true;
            } catch (ClientErrorException | IOException clientError) {
                log.info(clientError.getMessage());
                throw clientError;
            } catch (WebApplicationException e) {
                log.warn(e.getMessage(), e);
                throw e;
            } catch(Throwable t) {
                log.warn(t.getMessage(), t);
                throw new RuntimeException(t);
            } finally {
                if (! ready) {
                    if (submit.cancel(true)) {
                        log.debug("Canceled {}", submit);
                    }
                } else {
                    log.debug("let it end normally");
                }
            }
        };


        Response.ResponseBuilder builder =  Response.ok()
            .type(MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"))
            .entity(streamingOutput);

        ExtraHeaders.consume(builder::header);

        for (Consumer<Response.ResponseBuilder> c : responseBuilderConsumer) {
            c.accept(builder);
        }
        return builder.build();
    }


    @FunctionalInterface
    public interface JsonConsumer<T> {
        void accept(CloseableIterator<T> stream, JsonGenerator jg) throws Exception;
    }

    public static Function<Character, Boolean> keepAlive(JsonGenerator jg) {
        return c -> {
            try {
                jg.writeRaw(c);
                jg.flush();
                return false;
            } catch (IOException e) { // Client Aborted?
                log.warn(e.getClass().getName() + " " + e.getMessage());
                return true;
            }
        };
    }

    public static <T> void iterate(
        @NonNull Iterator<T> i,
        @NonNull JsonGenerator jg,
        @NonNull String fieldName,
        @NonNull String forString) throws IOException {

        jg.writeStartObject();
        jg.writeArrayFieldStart(fieldName);

        final Instant start = Instant.now();
        long count = 0;
        long errors = 0;

        try {
            while (i.hasNext()) {
                try {
                    T mo = i.next();
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
            jg.close();
        }
        Duration duration = Duration.between(start, Instant.now());
        log.info("Streamed for {} entries: {}, errors: {}. Took {}", forString, count, errors, duration);

    }
}
