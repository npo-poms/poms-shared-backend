/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.rs.error.NotFoundException;
import nl.vpro.api.rs.error.ServerErrorException;
import nl.vpro.api.rs.util.TestBean;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.UgcService;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.transfer.ugc.annotation.Annotations;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;


/**
 * User: rico
 * Date: 09/03/2012
 */

@Path("/media")
@Controller
public class Media {
    Logger logger = LoggerFactory.getLogger(Media.class);
    MediaService mediaService;

    UgcService ugcService;

    @Autowired
    public Media(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Autowired
    public void setUgcService(UgcService ugcService) {
        this.ugcService = ugcService;
    }

    @GET
    @Path("program/{urn}")
    public Program getProgram(@PathParam("urn") String urn) throws IllegalArgumentException {
        logger.debug("Called with urn " + urn);
        return mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
    }

    /**
     * Currently this only fetches the default annotation from the poms segments.
     *
     * @param urn
     * @return
     * @throws IllegalArgumentException
     */
    @GET
    @Path("program/{urn}/annotations")
    public Annotations getAnnotationsForProgram(@PathParam("urn") String urn) throws IllegalArgumentException {
        Annotation annotation = mediaService.getProgramAnnotation(MediaUtil.getMediaId(MediaObjectType.program, urn));
        Annotations annotations = new Annotations();
        if (annotation != null) {
            annotations.addAnnotation(annotation);
            annotations.setDefaultAnnotationIndex(0);
        }
        return annotations;
    }

    @GET
    @Path("group/{urn}")
    public Group getGroup(@PathParam("urn") String urn, @QueryParam("members") @DefaultValue("false") boolean addMembers) throws IllegalArgumentException, ServerErrorException, NotFoundException {
        logger.debug("Called with urn " + urn);
        return mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, urn), addMembers);
    }

    @GET
    @Path("segment/{urn}")
    public Segment getSegment(@PathParam("urn") String urn) throws IllegalArgumentException, ServerErrorException, NotFoundException {
        logger.debug("Called with urn " + urn);
        return mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, urn));
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
    public Annotation test() {

        Annotation a = ugcService.test("uri:vpro:ugc:annotation:123");
        return a;
    }

    private long getMediaId(String type, String urn) throws IllegalArgumentException {
        if (urn.matches("[0-9]+]")) {
            return Long.parseLong(urn);
        }
        if (urn.matches("urn:vpro:media:" + type + ":[0-9]+")) {
            return Long.parseLong(StringUtils.substringAfterLast(urn, ":"));
        }
        throw new IllegalArgumentException("urn " + urn + " could not be parsed to a valid id for an object of type " + type);
    }
}
