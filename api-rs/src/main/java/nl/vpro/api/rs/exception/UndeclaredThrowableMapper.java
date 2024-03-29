package nl.vpro.api.rs.exception;

import java.lang.reflect.UndeclaredThrowableException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Provider
public class UndeclaredThrowableMapper implements ExceptionMapper<UndeclaredThrowableException> {

    @Override
    public Response toResponse(UndeclaredThrowableException exception) {
        return Response
            .status(INTERNAL_SERVER_ERROR)
            .entity(new nl.vpro.domain.api.Error(INTERNAL_SERVER_ERROR, exception.getCause()))
                .build();
    }
}
