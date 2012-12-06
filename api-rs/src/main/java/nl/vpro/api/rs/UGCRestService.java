/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;

import javax.ws.rs.*;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path(UGCRestService.PATH)
public interface UGCRestService {
    public static final String PATH = "ugc";

    @GET
    @Path("playerconfiguration/{urn}")
    @Produces("application/json")
    public PlayerConfiguration getPlayerConfiguration(@PathParam("urn") String id);

    @POST
    @Path("playerconfiguration")
    @Produces("application/json")
    public PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration);

    @GET
    @Path("annotation/{urn}")
    @Produces("application/json")
    public Annotation getAnnotation(@PathParam("urn") String id);

    @GET
    @Path("annotation/bypart/{urn}")
    @Produces("application/json")
    public Annotation getAnnotationByPart(@PathParam("urn") String id);

}
