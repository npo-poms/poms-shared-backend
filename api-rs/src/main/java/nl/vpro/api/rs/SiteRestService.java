/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.SearchQuery;
import nl.vpro.api.transfer.SearchSuggestions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path(SiteRestService.PATH)
public interface SiteRestService {
    public static final String PATH = "site";

    @GET
    @Path("{profile}/view")
    @Produces("application/json")
    public GenericSearchResult view(@PathParam("profile") String profileName,  @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("max") Integer maxResult, @QueryParam("constraints") List<String> constraints, @QueryParam("facets") List<String> facets, @QueryParam("order") List<String> sortFields);

    @GET
    @Path("{profile}/search")
    @Produces("application/json")
    public GenericSearchResult search(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("max") Integer maxResult, @QueryParam("constraints") List<String> constraints, @QueryParam("facets") List<String> facets,@QueryParam("order") List<String> sortFields,  @QueryParam("response") String response);


    @GET
    @Path("{profile}/search/suggest")
    @Produces("application/json")
    public SearchSuggestions searchSuggestions(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("constraints") List<String> constraints);

    @POST
    @Path("{profile}/search")
    @Consumes("application/json")
    @Produces("application/json")
    public GenericSearchResult search(@PathParam("profile") String profileName, SearchQuery searchQuery);

    @POST
    @Path("{profile}/search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json")
    public GenericSearchResult search(@PathParam("profile") String profileName, @FormParam("query") String searchQueryAsJson);
}
