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
import nl.vpro.api.rs.util.StringUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.searchfilterbuilder.BooleanOp;
import nl.vpro.api.service.searchfilterbuilder.TagFilter;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Offers REST access to api.service.MediaService
 * User: rico
 * Date: 09/03/2012
 */

@Path(MediaRestServiceImpl.PATH)
@Controller
public class MediaRestServiceImpl implements MediaRestService {
    public static final String PATH = "media";
    Logger logger = LoggerFactory.getLogger(MediaRestServiceImpl.class);
    MediaService mediaService;

    @Autowired
    /**
     * inject the media service impl that does the real job
     */
    public MediaRestServiceImpl(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Override
    @GET
    @Path("program/{urn}")
    public Program getProgram(@PathParam("urn") String urn) throws IllegalArgumentException {
        logger.debug("Called with urn " + urn);
        return mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
    }

    @Override
    @GET
    @Path("program/replay")
    public ProgramList getRecentReplayablePrograms(@QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset) {
//    public MediaObjectList<Program> getRecentReplayablePrograms() {
        return mediaService.getReplayablePrograms(maxResult, offset);
    }

    /**
     * Currently this only fetches the default annotation from the poms segments.
     *
     * @param urn
     * @return
     * @throws IllegalArgumentException
     */
    @Override
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

    @Override
    @GET
    @Path("group/{urn}")
    /**
     * if members=true, members are added.
     * You can filter the types of members that may be returned, by added a membertypes request parameter, which contains comma-seperated list of allowed mediatypes.
     * The values of the media types must be the toString values of class MediaObjectType,
     * No filtering is obtained by either:
     * - not passing req.param membertypes
     * - leaving req.param membertypes an empty value: &membertypes=
     * - adding all possibile mediaObjectTypes: &membertypes=program,group,segment
     */
    public Group getGroup(@PathParam("urn") String urn, @QueryParam("members") @DefaultValue("false") boolean addMembers, @QueryParam("membertypes") String memberTypesFilter) throws ServerErrorException, NotFoundException {
        logger.debug("Called with urn " + urn);

        List<MediaObjectType> mediaObjectTypesFilter = null;

        if (addMembers && memberTypesFilter != null && !memberTypesFilter.isEmpty()) {
            //the caller wants members of specific types
            mediaObjectTypesFilter = new ArrayList<MediaObjectType>();
            List<String> allowedMediaTypes = Arrays.asList(memberTypesFilter.split(","));
            for (String memberType : allowedMediaTypes) {
                try {
                    mediaObjectTypesFilter.add(MediaObjectType.valueOf(memberType));
                } catch (java.lang.IllegalArgumentException iae) {
                    logger.error("Bij het opvragen van een Group aan de API Media Server werd een ongeldige filterwaarde op in membertypes meegegeven, namelijk: " + memberType + ". Dit wordt daarom genegeerd als filterwaarde.");
                }
            }
        }

        return mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, urn), addMembers, mediaObjectTypesFilter);
    }

    @Override
    @GET
    @Path("segment/{urn}")
    public Segment getSegment(@PathParam("urn") String urn) throws ServerErrorException, NotFoundException {
        logger.debug("Called with urn " + urn);
        return mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, urn));
    }

    @Override
    @GET
    @Path("search/{profile}")
    public MediaSearchResult searchWithProfile(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, profileName, offset, maxResult);
    }

    @Override
    @GET
    @Path("search")
    public MediaSearchResult search(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, "", offset, maxResult);
    }

    @Override
    @GET
    @Path("search/suggest")
    public MediaSearchSuggestions searchSuggestions(@QueryParam("q") String queryString, @QueryParam("tags") String tags) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(queryString, tagFilter, "");
    }

    @Override
    @GET
    @Path("search/suggest/{profile}")
    public MediaSearchSuggestions searchSuggestionsWithProfile(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @PathParam("profile") String profileName) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(queryString, tagFilter, profileName);
    }

    private TagFilter createFilter(String tags, BooleanOp booleanOp) {
        TagFilter tagFilter = null;
        if (StringUtils.isNotBlank(tags)) {
            tagFilter = new TagFilter((booleanOp));
            for (String tag : StringUtil.split(tags)) { //tags.split(" ") is not enough, because tags can have spaces.
                tagFilter.addTag(tag);
            }
        }
        return tagFilter;
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
