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

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Path(PageRestService.PATH)
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface PageRestService {
    public static final String PATH = "/pages";

    @GET
    PageResult list(
        @QueryParam("pf") String profile,
        @QueryParam("o") @DefaultValue("0") Long offset,
        @QueryParam("m") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

    @POST
    PageSearchResult find(
        PageForm form,
        @QueryParam("pf") String profile,
        @QueryParam("o") @DefaultValue("0") Long offset,
        @QueryParam("m") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @QueryParam("mock") @DefaultValue("false") boolean mock);

}
