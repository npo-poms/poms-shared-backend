/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.page.PageForm;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Path(PageRestService.PATH)
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface PageRestService {
    public static final String PATH = "/pages";

    @GET
    Response list(
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max);

    @POST
    Response find(
        PageForm form,
        @QueryParam("profile") String profile,
        @QueryParam("properties") String properties,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max);

}
