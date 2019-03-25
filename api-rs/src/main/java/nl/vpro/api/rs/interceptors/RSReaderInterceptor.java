package nl.vpro.api.rs.interceptors;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

/**
 * @author Michiel Meeuwissen
 * @since 4.1
 */
@Provider
@Slf4j
public class RSReaderInterceptor implements ReaderInterceptor {

    public static ThreadLocal<Boolean> READEXCEPTION = ThreadLocal.withInitial(() -> false);

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        try {
            READEXCEPTION.set(false);
            log.debug("Start {}", context);
            return context.proceed();
        } catch (IOException | WebApplicationException readException) {
            READEXCEPTION.set(true);
            throw readException;
        } finally {
            log.debug("Stop {}", context);
        }

    }
}
