/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path("site")
public interface SiteRestService {

    @GET
    @Path("{profile}/search")
    public GenericSearchResult search(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult, @QueryParam("constraints") List<String> constraints, @QueryParam("facets") List<String> facets,  @QueryParam("response") String response);


    @GET
    @Path("{profile}/search/suggest")
    public MediaSearchSuggestions searchSuggestions(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("constraints") List<String> constraints);

}
