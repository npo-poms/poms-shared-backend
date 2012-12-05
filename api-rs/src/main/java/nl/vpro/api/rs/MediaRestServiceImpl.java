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
import nl.vpro.api.service.search.fiterbuilder.BooleanOp;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResultItem;
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

import java.util.*;

/**
 * Offers REST access to api.service.MediaService
 * User: rico
 * Date: 09/03/2012
 */
@Path(MediaRestServiceImpl.PATH)
@Controller
public class MediaRestServiceImpl implements MediaRestService {

    public static final String PATH = "media";
    private final static Logger logger = LoggerFactory.getLogger(MediaRestServiceImpl.class);

    @Autowired
    MediaService mediaService;

    @Override
    @GET
    @Path("program/{urn}")
    public Program getProgram(@PathParam("urn") String urn) throws IllegalArgumentException {
        logger.debug("Method getProgram called with urn " + urn);
        return mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
    }

    @Override
    @GET
    @Path("program/replay")
    public ProgramList getRecentReplayablePrograms(@QueryParam("max") Integer maxResult, @QueryParam("offset") Integer offset, @QueryParam("type") String avType) {
        return mediaService.getReplayablePrograms(maxResult, offset, avType);
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
    public Group getGroup(String urn, boolean addMembers, String membersTypeFilter) {
        return getGroup(urn, addMembers, false, membersTypeFilter);
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
    public Group getGroup(@PathParam("urn") String urn, @QueryParam("members") @DefaultValue("false") boolean addMembers, @QueryParam("episodes") @DefaultValue("false") boolean addEpisodes, @QueryParam("membertypes") String memberTypesFilter) throws ServerErrorException, NotFoundException {
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

        return mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, urn), addMembers, addEpisodes, mediaObjectTypesFilter);
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
    public MediaSearchResult searchWithProfile(@PathParam("profile") String profileName, @QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, profileName, offset, maxResult);
    }

    @Override
    @GET
    @Path("search")
    public MediaSearchResult search(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, "", offset, maxResult);
    }

    @Override
    @GET
    @Path("search/suggest")
    public MediaSearchSuggestions searchSuggestions(@QueryParam("q") String termPrefix, @QueryParam("tags") String tags) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(termPrefix, tagFilter, "");
    }

    @Override
    @GET
    @Path("search/suggest/{profile}")
    public MediaSearchSuggestions searchSuggestionsWithProfile(@QueryParam("q") String queryString, @QueryParam("tags") String tags, @PathParam("profile") String profileName) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(queryString, tagFilter, profileName);
    }

    @Override
    @GET
    @Path("related/{urn}")
    public ProgramList related(@PathParam("urn") String urn, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult) throws IllegalArgumentException {
        return relatedWithProfile(StringUtils.EMPTY, urn, offset, maxResult);
    }

    @Override
    @GET
    @Path("related/{profile}/{urn}")
    public ProgramList relatedWithProfile(@PathParam("profile") String profile, @PathParam("urn") String urn, @QueryParam("offset") Integer offset, @QueryParam("max") Integer maxResult) throws IllegalArgumentException {
        Long id = MediaUtil.getMediaId(MediaObjectType.program, urn); // TODO: Segments? Groups?
        Program program = mediaService.getProgram(id);

        List<String> tags = program.getTags();
        TagFilter tagFilter = new TagFilter(BooleanOp.OR);
        for (String tag : tags) {
            tagFilter.addTag(tag);
        }

        MediaSearchResult mediaSearchResult = mediaService.search(StringUtils.EMPTY, tagFilter, profile, offset, maxResult);

        long numFound = mediaSearchResult.getNumFound();
        long start = mediaSearchResult.getStart();
        ProgramList programList = new ProgramList(numFound, start); // TODO: num found is momenteel niet correct omdat er nog MediaSearchResultItems genegeerd worden

        List<MediaSearchResultItem> mediaSearchResultItems = mediaSearchResult.getMediaSearchResultItems();
        for (MediaSearchResultItem mediaSearchResultItem : mediaSearchResultItems) {
            String relatedUrn = mediaSearchResultItem.getUrn();
            try {
                Long relatedId = MediaUtil.getMediaId(MediaObjectType.program, relatedUrn); // TODO: Segments? Groups?
                Program relatedProgram = mediaService.getProgram(relatedId);
                programList.addProgram(relatedProgram);
            }
            catch (IllegalArgumentException e) {
                // TODO: Ignore, we only consider programs at the moment
            }
        }

        return programList;
    }

    @Override
    @POST
    @Path("search/es/{index}")
    @Deprecated
    public String searchES(@PathParam("index") String index, @FormParam("query") String query, @FormParam("documentTypes") String typesAsString) {
        String[] types = typesAsString.trim().split(" ");
        return mediaService.searchES(index, types, query);
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
