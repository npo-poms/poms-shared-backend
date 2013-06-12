/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.PageResult;
import nl.vpro.domain.api.PageSearchResult;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.page.Page;

/**
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Path(PageRestService.PATH)
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface PageRestService {
    public static final String PATH = "/pages";


    @GET
    @Path("/")
    PageResult list(
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    @Path("/")
    PageSearchResult find(
        PageForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * Retrieves a page by it's id.
     *
     * @param id   existing id for a page
     * @param mock whether to return a mock object
     */
    @GET
    @Path("/{id}")
    Page load(
        @PathParam("id") String id,
        @QueryParam("mock") @DefaultValue("false") boolean mock);


}
