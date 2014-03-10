/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.vpro.domain.api.Constants;

/**
 * @author Roelof Jan Koekoek
 * @since 2.2
 */
@Path(StatsRestService.PATH)
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface StatsRestService {
    public static final String PATH = "/stats";

    @GET
    @Path("/media")
    Response stats(
        @QueryParam("period") @DefaultValue("hour") String period,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/media/broadcasters/{broadcaster}")
    Response statsPerBroadcaster(
        @PathParam("broadcaster") String broadcaster,
        @QueryParam("period") @DefaultValue("hour") String period,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/media/portals/{portal}")
    Response statsPerPortal(
        @PathParam("portal") String portal,
        @QueryParam("period") @DefaultValue("hour") String period,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

}
