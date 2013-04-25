/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.api.domain.media.*;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.rs.util.StringUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.search.filterbuilder.BooleanOp;
import nl.vpro.api.service.search.filterbuilder.TagFilter;
import nl.vpro.api.transfer.*;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.WrappedIterator;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Offers REST access to api.service.MediaService
 * User: rico
 * Date: 09/03/2012
 */
@Controller
public class MediaRestServiceImpl implements MediaRestService {

    private final static Logger LOG = LoggerFactory.getLogger(MediaRestServiceImpl.class);

    @Autowired
    MediaService mediaService;


    @Autowired
    ConversionService conversionService;

    @Override
    public Program getProgram(String urn) throws IllegalArgumentException {
        LOG.debug("Method getProgram called with urn {}", urn);
        return mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
    }

    @Override
    public List<MediaObject> relatedForProgram(String programUrn, String profile, Integer offset, Integer maxResult) {
        MediaObject mediaObject = mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, programUrn));
        return relatedForMediaObject(mediaObject, profile, offset, maxResult);
    }

    @Override
    public ProgramList getRecentReplayablePrograms(Integer maxResult, Integer offset, String avType) {
        return mediaService.getReplayablePrograms(maxResult, offset, StringUtils.isEmpty(avType) ? null : AvType.valueOf(avType.toUpperCase()));
    }

    @Override
    public MediaSearchResultItemIterator getAllReplayableProgram(final String avType) {
        return new MediaSearchResultItemIterator(new WrappedIterator<Program, MediaSearchResultItem>(mediaService.getAllReplayablePrograms(getAvType(avType))) {
            @Override
            public MediaSearchResultItem next() {
                return new MediaSearchResultItem(wrapped.next());
            }
        });
    }

    private AvType getAvType(String avType) {
        return StringUtils.isEmpty(avType) ? null : AvType.valueOf(avType.toUpperCase());
    }

    /**
     * Currently this only fetches the default annotation from the poms segments.
     *
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
    /**
     * if members=true, members are added.
     * You can filter the types of members that may be returned, by added a membertypes request parameter, which contains comma-seperated list of allowed mediatypes.
     * The values of the media types must be the toString values of class MediaObjectType,
     * No filtering is obtained by either:
     * - not passing req.param membertypes
     * - leaving req.param membertypes an empty value: &membertypes=
     * - adding all possibile mediaObjectTypes: &membertypes=program,group,segment
     */
    public Group getGroup(
            String urn,
            boolean addMembers,
            boolean addEpisodes,
            String memberTypesFilter) throws ServerErrorException, NotFoundException {
        LOG.debug("Called with urn " + urn);

        List<MediaObjectType> mediaObjectTypesFilter = null;

        if (addMembers && memberTypesFilter != null && !memberTypesFilter.isEmpty()) {
            //the caller wants members of specific types
            mediaObjectTypesFilter = new ArrayList<MediaObjectType>();
            List<String> allowedMediaTypes = Arrays.asList(memberTypesFilter.split(","));
            for (String memberType : allowedMediaTypes) {
                try {
                    mediaObjectTypesFilter.add(MediaObjectType.valueOf(memberType));
                } catch (java.lang.IllegalArgumentException iae) {
                    LOG.error("Bij het opvragen van een Group aan de API Media Server werd een ongeldige filterwaarde op in membertypes meegegeven, namelijk: " + memberType + ". Dit wordt daarom genegeerd als filterwaarde.");
                }
            }
        }

        return mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, urn), addMembers, addEpisodes, mediaObjectTypesFilter);
    }

    @Override
    public List<MediaObject> relatedForSegment(String segmentUrn, String profile, Integer offset, Integer maxResult) {
        MediaObject mediaObject = mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, segmentUrn));
        return relatedForMediaObject(mediaObject, profile, offset, maxResult);
    }

    @Override
    public Segment getSegment(String urn) throws ServerErrorException, NotFoundException {
        LOG.debug("Called with urn " + urn);
        return mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, urn));
    }

    @Override
    public List<MediaObject> relatedForGroup(String groupUrn, String profile, Integer offset, Integer maxResult) {
        MediaObject mediaObject = mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, groupUrn), false, null);
        return relatedForMediaObject(mediaObject, profile, offset, maxResult);
    }

    @Override
    public List<MediaObject> getMediaObjects(List<String> urns) {
        List<MediaObject> mediaObjects = new ArrayList<MediaObject>();
        for (String urn : urns) {
            try {
                MediaObject mediaObject = getMedia(urn);
                mediaObjects.add(mediaObject);
            } catch (Exception e) {
                LOG.warn("Could not add media object with urn '{}' to result of bulk retrieve call: {}", urn, e.getMessage());
            }
        }
        return mediaObjects;
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
    public MediaSearchResultItemIterator getProfile(String profileName) {
        return new MediaSearchResultItemIterator(mediaService.getProfile(profileName));
    }

    @Override
    public SearchSuggestions searchSuggestions(String termPrefix, String tags) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(termPrefix, tagFilter, "");
    }

    @Override
    public SearchSuggestions searchSuggestionsWithProfile(String queryString, String tags, String profileName) {
        TagFilter tagFilter = createFilter(tags, BooleanOp.OR);
        return mediaService.searchSuggestions(queryString, tagFilter, profileName);
    }

    @Override
    @Deprecated
    public String searchES(String index,String query, String typesAsString) {
        String[] types = null;
        if (typesAsString != null) {
            types = typesAsString.trim().split(" ");
        }
        return mediaService.searchES(index, types, query);
    }

    /*
    @Override
    public Subtitle searchSubtitles(@PathParam("urn") String urn, String term) {
        //mediaService.searchSubtitles(urn, term);
        return new Subtitle(0, "urn:bla", "floe");

    }
    */

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

    private List<MediaObject> relatedForMediaObject(MediaObject mediaObject, String profile, Integer offset, Integer maxResult) {
        List<String> tags = mediaObject.getTags();
        TagFilter tagFilter = new TagFilter(BooleanOp.OR);
        for (String tag : tags) {
            tagFilter.addTag(tag);
        }

        MediaSearchResult mediaSearchResult = mediaService.search(StringUtils.EMPTY, tagFilter, profile, offset, maxResult != null ? maxResult + 1 : null); // We could find ourself in the results, so search for one more to be sure
        List<MediaSearchResultItem> mediaSearchResultItems = mediaSearchResult.getMediaSearchResultItems();

        String urn = mediaObject.getUrn();
        List<MediaObject> relatedMediaObjects = new ArrayList<MediaObject>();
        for (MediaSearchResultItem mediaSearchResultItem : mediaSearchResultItems) {
            if (maxResult != null && relatedMediaObjects.size() >= maxResult)
                break; // Since we searched for one more than maxResult, we could already be done
            String relatedUrn = mediaSearchResultItem.getUrn();
            if (!urn.equals(relatedUrn)) {
                try {
                    MediaObject relatedMediaObject = getMedia(relatedUrn);
                    relatedMediaObjects.add(relatedMediaObject);
                } catch (Exception e) {
                    LOG.warn("Could not retrieve related media object {}: {}", relatedUrn, e.getMessage());
                }
            }
        }
        return relatedMediaObjects;
    }

    private MediaObject getMedia(String urn) {
        MediaObject mediaObject;
        MediaObjectType mediaObjectType = MediaUtil.getMediaType(urn);
        if (MediaObjectType.program.equals(mediaObjectType)) {
            mediaObject = mediaService.getProgram(MediaUtil.getMediaId(MediaObjectType.program, urn));
        } else if (MediaObjectType.segment.equals(mediaObjectType)) {
            mediaObject = mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, urn));
        } else if (MediaObjectType.group.equals(mediaObjectType)) {
            mediaObject = mediaService.getGroup(MediaUtil.getMediaId(MediaObjectType.group, urn), false, null);
        } else {
            throw new RuntimeException("Unknown media object type: " + mediaObjectType);
        }
        return mediaObject;
    }
}
