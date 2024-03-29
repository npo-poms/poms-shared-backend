package nl.vpro.api.rs.exception;

import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Failure;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
@Log4j2
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if(exception instanceof Failure) {
            // E.g. a DefaultOptionsMethodException. If you catch those, CORS wont' work any more.
            return null;
        } else {
            Throwable t = exception;
            while(t.getCause() != null) {
                t = t.getCause();
            }
            log.warn("Wrapped an {} root cause {}. Request: {}", t.getClass().getName(), t.getMessage(), StoreRequestInThreadLocal.getRequestBody());
            return Response
                    .serverError()
                    .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                    .build();
        }
    }
}
