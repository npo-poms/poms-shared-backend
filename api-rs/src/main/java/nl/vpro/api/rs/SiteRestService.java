/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import java.util.List;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path(SiteRestService.PATH)
public interface SiteRestService {
    public static final String PATH = "site";

    @GET
    @Path("{profile}/search")
    @Produces("application/json")
    public GenericSearchResult search(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("max") Integer maxResult, @QueryParam("constraints") List<String> constraints, @QueryParam("facets") List<String> facets,@QueryParam("order") List<String> sortFields,  @QueryParam("response") String response);


    @GET
    @Path("{profile}/search/suggest")
    @Produces("application/json")
    public MediaSearchSuggestions searchSuggestions(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("constraints") List<String> constraints);

}
