/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Path("/profiles")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface ProfileRestService {

}
