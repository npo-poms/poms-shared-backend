/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.pages.PageForm;
import nl.vpro.domain.pages.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Path("/pages")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface PageRestService {

    @GET
    @Path("/")
    PagedResult<Page> list(
            @QueryParam("profile") String profile,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("limit") @DefaultValue("50") Integer limit,
            @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    @Path("/")
    PagedResult<Page> search(
            PageForm form,
            @QueryParam("profile") String profile,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("limit") @DefaultValue("50") Integer limit,
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
