/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import nl.vpro.api.media.MediaService;
import nl.vpro.api.media.SearchService;
import nl.vpro.api.media.search.SearchResult;
import nl.vpro.api.rs.util.TestBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;


/**
 * User: rico
 * Date: 09/03/2012
 */

@Path("/media")
@Controller
public class Media {
    Logger logger = LoggerFactory.getLogger(Media.class);
    MediaService mediaService;
    SearchService searchService;

    @Autowired
    public Media(MediaService mediaService, SearchService searchService) {
        this.mediaService = mediaService;
        this.searchService = searchService;
    }

    @GET
    @Path("item/{urn}")
    public String getMedia(@PathParam("urn") String urn) {
        logger.debug("Called with param " + urn);
        return mediaService.get(urn);
    }

    @GET
    @Path("find/bygroup/{group}")
    public SearchResult findItemsInGroup(@PathParam("group") String group){
        if(group.matches("^\\d+$")){
            group = "urn:vpro:media:group:" + group;
        }
        return searchService.searchMediaWithAncestor(group, null, -1, -1);
    }

    @GET
    @Path("/test")
    public TestBean test(){
        return new TestBean();
    }
}
