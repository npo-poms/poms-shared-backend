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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.ThreadPools;

/**
 * @author Michiel Meeuwissen
 * @since 5.4
 */
@Slf4j
public class Iterate {

    @SafeVarargs
    public static <T> Response streamingJson(Function<JsonGenerator, CloseableIterator<T>> creator, JsonConsumer<T> streamer, Consumer<Response.ResponseBuilder>... responseBuilderConsumer) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        pipedInputStream.connect(pipedOutputStream);

        JsonGenerator jg = Jackson2Mapper.INSTANCE.getFactory().createGenerator(pipedOutputStream);

        CloseableIterator<T> iterator;
        try {
            iterator = creator.apply(jg);
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
        });

        final StreamingOutput streamingOutput = output -> {
            try {
                IOUtils.copy(pipedInputStream, output);
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
                if (submit.cancel(true)) {
                    log.debug("Canceled {}", submit);
                }
            }
        };


        Response.ResponseBuilder builder =  Response.ok()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(streamingOutput);
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

    public static <T> void iterate(Iterator<T> i, JsonGenerator jg, String fieldName, String forString) throws IOException {
        jg.writeStartObject();
        jg.writeArrayFieldStart(fieldName);

        Instant start = Instant.now();
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
        }
        Duration duration = Duration.between(start, Instant.now());
        log.info("Streamed for {} entries: {}, errors: {}. Took {}", forString, count, errors, duration);

    }
}
