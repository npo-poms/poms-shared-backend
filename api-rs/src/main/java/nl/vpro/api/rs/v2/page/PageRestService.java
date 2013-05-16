/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.api.pages.Page;
import nl.vpro.domain.api.pages.PageForm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
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
     * Retrieve a media resource, either a Program, Group or Segment, by it's id.
     *
     * @param id   existing urn or mid
     * @param mock whether to return a mock object
     * @return full Program, Group or Segment
     */
    @GET
    @Path("/{id}")
    Page get(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock);



}
