package nl.vpro.api.rs.error;

import nl.vpro.api.transfer.ErrorResponse;

import javax.ws.rs.core.Response;

/**
 * Date: 23-4-12
 * Time: 10:48
 *
 * @author Ernst Bunders
 */
public abstract class ApiErrorMapper {
    protected Response createResponse(String message, int status) {
        return Response
            .status(status)
            .entity(new ErrorResponse(message, status))
            .build();
    }
}
