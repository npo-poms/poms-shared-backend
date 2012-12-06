/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.rs.util.StringUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.search.fiterbuilder.BooleanOp;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItem;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.ProgramList;
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

import static nl.vpro.api.domain.media.support.MediaObjectType.*;

/**
 * Offers REST access to api.service.MediaService
 * User: rico
 * Date: 09/03/2012
 */
@Controller
public class MediaRestServiceImpl implements MediaRestService {

    private final static Logger logger = LoggerFactory.getLogger(MediaRestServiceImpl.class);

    @Autowired
    MediaService mediaService;

    @Override
    public Program getProgram(String urn) throws IllegalArgumentException {
        logger.debug("Method getProgram called with urn " + urn);
        return mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
    }

    @Override
    public ProgramList getRecentReplayablePrograms(Integer maxResult, Integer offset, String avType) {
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
    public Annotations getAnnotationsForProgram(String urn) throws IllegalArgumentException {
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
    /**
     * if members=true, members are added.
     * You can filter the types of members that may be returned, by added a membertypes request parameter, which contains comma-seperated list of allowed mediatypes.
     * The values of the media types must be the toString values of class MediaObjectType,
     * No filtering is obtained by either:
     * - not passing req.param membertypes
     * - leaving req.param membertypes an empty value: &membertypes=
     * - adding all possibile mediaObjectTypes: &membertypes=program,group,segment
     */
    public Group getGroup(String urn, boolean addMembers, boolean addEpisodes, String memberTypesFilter) throws ServerErrorException, NotFoundException {
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
    public Segment getSegment(String urn) throws ServerErrorException, NotFoundException {
        logger.debug("Called with urn " + urn);
        return mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, urn));
    }

    @Override
    public MediaSearchResult search(String queryString, String tags, Integer offset, Integer maxResult) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, "", offset, maxResult);
    }

    @Override
    public MediaSearchResult searchWithProfile(String profileName, String queryString, String tags, Integer offset, Integer maxResult) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.search(queryString, tagFilter, profileName, offset, maxResult);
    }

    @Override
    public MediaSearchSuggestions searchSuggestions(String termPrefix, String tags) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(termPrefix, tagFilter, "");
    }

    @Override
    public MediaSearchSuggestions searchSuggestionsWithProfile(String queryString, String tags, String profileName) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(queryString, tagFilter, profileName);
    }

    @Override
    public List<MediaObject> related(String urn, Integer offset, Integer maxResult) throws IllegalArgumentException {
        return relatedWithProfile(StringUtils.EMPTY, urn, offset, maxResult);
    }

    @Override
    public List<MediaObject> relatedWithProfile(String profile, String urn, Integer offset, Integer maxResult) throws IllegalArgumentException {
        MediaObject mediaObject = getMedia(urn);

        List<String> tags = mediaObject.getTags();
        TagFilter tagFilter = new TagFilter(BooleanOp.OR);
        for (String tag : tags) {
            tagFilter.addTag(tag);
        }

        MediaSearchResult mediaSearchResult = mediaService.search(StringUtils.EMPTY, tagFilter, profile, offset, maxResult + 1); // We could find ourself in the results, so search for one more to be sure
        List<MediaSearchResultItem> mediaSearchResultItems = mediaSearchResult.getMediaSearchResultItems();

        List<MediaObject> relatedMediaObjects = new ArrayList<MediaObject>();
        for (MediaSearchResultItem mediaSearchResultItem : mediaSearchResultItems) {
            if (relatedMediaObjects.size() >= maxResult) break; // Since we searched for one more than maxResult, we could already be done
            String relatedUrn = mediaSearchResultItem.getUrn();
            if (! urn.equals(relatedUrn)) {
                try {
                    MediaObject relatedMediaObject = getMedia(relatedUrn);
                    relatedMediaObjects.add(relatedMediaObject);
                } catch (Exception e) {
                    logger.warn("Could not retrieve related media object " + relatedUrn, e);
                }
            }
        }

        return relatedMediaObjects;
    }

    @Override
    public String searchES(String index,String query, String typesAsString) {
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

    private MediaObject getMedia(String urn) {
        MediaObject mediaObject;
        MediaObjectType mediaObjectType = MediaUtil.getMediaType(urn);
        switch (mediaObjectType) {
            case program:
                mediaObject = mediaService.getProgram(MediaUtil.getMediaId(program, urn));
                break;
            case segment:
                mediaObject = mediaService.getSegment(MediaUtil.getMediaId(segment, urn));
                break;
            case group:
                mediaObject = mediaService.getGroup(MediaUtil.getMediaId(group, urn), false, null);
                break;
            default:
                throw new RuntimeException("Unknown media object type: " + mediaObjectType);
        }
        return mediaObject;
    }
}