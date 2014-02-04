package nl.vpro.api.rs.v2;

import nl.vpro.api.rs.v2.exception.BadRequest;
import nl.vpro.api.rs.v2.exception.ServerError;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Deprecated
public class Util {

    public static RuntimeException exception(Exception e) {
        if (e instanceof ProfileNotFoundException) {
            return new BadRequest(e);
        } else {
            return new ServerError(e);
        }
    }
}
