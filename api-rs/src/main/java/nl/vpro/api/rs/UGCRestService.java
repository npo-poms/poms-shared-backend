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
import javax.ws.rs.core.MediaType;

/**
 * User: rico
 * Date: 28/11/2012
 */
@Path(UGCRestService.PATH)
public interface UGCRestService {
    public static final String PATH = "ugc";


    @GET
    @Path("annotation/{urn}")
    @Produces(MediaType.APPLICATION_JSON+"; charset=UTF-8")
    public Annotation getAnnotation(@PathParam("urn") String id);

    @GET
    @Path("annotation/bypart/{urn}")
    @Produces(MediaType.APPLICATION_JSON+"; charset=UTF-8")
    public Annotation getAnnotationByPart(@PathParam("urn") String id);

}
