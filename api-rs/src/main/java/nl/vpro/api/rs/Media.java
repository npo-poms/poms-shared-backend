/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.rs.util.TestBean;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;


/**
 * User: rico
 * Date: 09/03/2012
 */

@Path("/media")
@Controller
public class Media {
    Logger logger = LoggerFactory.getLogger(Media.class);
    MediaService mediaService;

    @Autowired
    public Media(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GET
    @Path("item/{urn}")
    public String getMedia(@PathParam("urn") String urn) {
        logger.debug("Called with param " + urn);
        return mediaService.get(urn);
    }

    @GET
    @Path("search/{profile}")
    public MediaSearchResult searchWithProfile(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset) {
        return mediaService.search(queryString, profileName, offset, maxResult);
    }

    @GET
    @Path("search")
    public MediaSearchResult search(@QueryParam("q") String queryString, @QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset) {
        return mediaService.search(queryString, "", offset, maxResult);
    }

    @GET
    @Path("search/suggest")
    public MediaSearchSuggestions searchSuggestions(@QueryParam("q") String queyString) {
        return mediaService.searchSuggestions(queyString, "");
    }

    @GET
    @Path("search/suggest/{profile}")
    public MediaSearchSuggestions searchSuggestionsWithProfile(@QueryParam("q") String queyString, @PathParam("profile") String profileName) {
        return mediaService.searchSuggestions(queyString, profileName);
    }

    @GET
    @Path("/test")
    public TestBean test() {

        return new TestBean();
    }
}
