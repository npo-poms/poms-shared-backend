package nl.vpro.api.rs.exception;

import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;

import nl.vpro.api.rs.interceptors.StoreRequestInThreadLocal;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author Michiel Meeuwissen
 * @since 8.4
 */
@Provider
@Log4j2
public class ElasticsearchStatusExceptionMapper implements ExceptionMapper<ElasticsearchException> {

    @Override
    public Response toResponse(ElasticsearchException exception) {
        if (exception instanceof ElasticsearchStatusException statusException) {
            return Response.status(statusException.status().getStatus())
                .entity(new nl.vpro.domain.api.Error(statusException.status().getStatus(), statusException)).build();
        }
        log.warn("ElasticSearch exception {} root cause {}. Request: {}", exception.getClass().getName(), exception.getMessage(), StoreRequestInThreadLocal.getRequestBody());
        return Response
            .serverError()
            .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception))
            .build();
    }
}
