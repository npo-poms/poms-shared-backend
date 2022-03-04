package nl.vpro.api.rs.exception;

import java.lang.reflect.UndeclaredThrowableException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

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
