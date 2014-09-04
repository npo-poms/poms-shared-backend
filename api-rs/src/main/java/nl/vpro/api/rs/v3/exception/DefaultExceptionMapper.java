package nl.vpro.api.rs.v3.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Catch all other exeption and show them as a server error
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        return Response.serverError().entity(new nl.vpro.domain.api.Error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getClass().getName() + ": " + exception.getMessage())).build();
    }

}


