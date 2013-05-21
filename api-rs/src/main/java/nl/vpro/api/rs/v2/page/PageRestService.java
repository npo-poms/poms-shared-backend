/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.page.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Path("/pages")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface PageRestService {
    static final String DEFAULT_MAX = "10";

    @GET
    @Path("/")
    Result<Page> list(
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
        @QueryParam("max") @DefaultValue(DEFAULT_MAX) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    @Path("/")
    Result<Page> search(
        PageForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
        @QueryParam("max") @DefaultValue(DEFAULT_MAX) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    /**
     * Retrieves a page by it's id.
     *
     * @param id   existing id for a page
     * @param mock whether to return a mock object
     */
    @GET
    @Path("/{id}")
    Page get(
        @PathParam("id") String id,
        @QueryParam("mock") @DefaultValue("false") boolean mock);


}
