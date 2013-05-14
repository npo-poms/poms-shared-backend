/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.domain.ugc.annotation.Annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path(UGCRestService.PATH)
public interface UGCRestService {
    public static final String PATH = "ugc";


    @GET
    @Path("annotation/{urn}")
    @Produces("application/json")
    public Annotation getAnnotation(@PathParam("urn") String id);

    @GET
    @Path("annotation/bypart/{urn}")
    @Produces("application/json")
    public Annotation getAnnotationByPart(@PathParam("urn") String id);

}
