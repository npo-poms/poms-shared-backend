package nl.vpro.api.rs.exception;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Failure;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
@Slf4j
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if(exception instanceof Failure) {
            // E.g. a DefaultOptionsMethodException. If you catch those, CORS wont' work any more.
            return null;
        } else {
            log.warn("Wrapped an {} root cause {}. Request: {}", exception.getClass().getName(), exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
            return Response
                    .serverError()
                    .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
                    .build();
        }
    }
}
