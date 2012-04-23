package nl.vpro.api.rs.error;

import nl.vpro.api.service.NotFoundException;
import nl.vpro.api.service.ServerErrorException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.ErrorManager;

/**
 * Date: 23-4-12
 * Time: 10:36
 *
 * @author Ernst Bunders
 */
@Provider
public class NotFoundExceptionMapper extends ApiErrorMapper implements ExceptionMapper<NotFoundException>  {

    public Response toResponse(NotFoundException exception) {
        return createResponse(exception.getMessage(), 404);
    }
}
