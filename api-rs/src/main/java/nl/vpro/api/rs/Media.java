/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import nl.vpro.api.media.MediaService;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;


/**
 * User: rico
 * Date: 09/03/2012
 */

@Path("/media")
@Controller
public class Media {
    Logger logger = LoggerFactory.getLogger(Media.class);
    @Autowired
    MediaService mediaService;

    @GET
    @Path("item/{urn}")
    public String getMedia(@PathParam("urn") String urn) {
          logger.debug("Called with param "+urn);
          return mediaService.get(urn);
    }
}
