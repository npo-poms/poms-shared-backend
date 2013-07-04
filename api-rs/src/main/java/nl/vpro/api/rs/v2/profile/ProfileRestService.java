/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import nl.vpro.domain.api.profile.Profile;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Simple service to view of inspect the available site profiles.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path(ProfileRestService.PATH)
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface ProfileRestService {
    public static final String PATH = "profiles";


    /**
     * Returns a site profile by it's key
     *
     * @param name an profile identifier
     * @return an existing profile or an error when no profile is found
     */
    @GET
    @Path("/{name}")
    Profile load(
        @PathParam("name") String name,
        @QueryParam("mock") @DefaultValue("false") boolean mock
    );
}
