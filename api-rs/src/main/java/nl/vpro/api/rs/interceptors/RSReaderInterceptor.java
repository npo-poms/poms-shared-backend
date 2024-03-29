package nl.vpro.api.rs.interceptors;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * @author Michiel Meeuwissen
 * @since 4.1
 */
@Provider
@Log4j2
public class RSReaderInterceptor implements ReaderInterceptor {

    public static final ThreadLocal<Boolean> READEXCEPTION = ThreadLocal.withInitial(() -> false);

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
