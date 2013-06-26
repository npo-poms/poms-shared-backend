/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;

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
@StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "bad request"),
    @ResponseCode(code = 500, condition = "server error")})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface PageRestService {
    public static final String PATH = "/pages";

    @GET
    PageResult list(
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    PageSearchResult find(
        PageForm form,
        @QueryParam("profile") String profile,
        @QueryParam("offset") @DefaultValue("0") Long offset,
        @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

}
