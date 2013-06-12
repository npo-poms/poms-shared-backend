/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wordnik.swagger.annotations.*;

import nl.vpro.api.rs.v2.exception.BadRequest;
import nl.vpro.api.rs.v2.exception.ServerError;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.media.MediaObject;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
@Api(value = MediaRestService.PATH, description = "The media API")
public class MediaRestServiceImpl implements MediaRestService {

    private final MockMediaRestService mocks = new MockMediaRestService();

    private final MediaService mediaService;

    @Autowired
    public MediaRestServiceImpl(MediaService mediaService) {
        this.mediaService = mediaService;

    }

    @ApiOperation(httpMethod = "get", value = "Get all media", notes = "Get all media filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 404, reason = "Server error")})
    @Override
    public MediaResult list(
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.list(profile, offset, max, true);
        }
        return find(null, profile, offset, max, mock).asResult();
    }

    @ApiOperation(httpMethod = "post", value = "Find media", notes = "Find media by posting a search form. Results are filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 404, reason = "Server error")})
    @Override
    public MediaSearchResult find(
        @ApiParam(value = "Search form", required = false, defaultValue = "{\n" +
            "   \"highlight\" : true,\n" +
            "   \"facets\" :\n" +
            "      {\n" +
            "      },\n" +
            "   \"searches\" :\n" +
            "      {\n" +
            "      }\n" +
            "}") MediaForm form,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.find(form, profile, offset, max, true);
        }
        try {
            return mediaService.find(profile, form, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public MediaObject load(String id, boolean mock) {
        if(mock) {
            return mocks.load(id, true);
        }
        MediaObject mediaObject;
        try {
            mediaObject = mediaService.load(id);
        } catch(Exception e) {
            throw new ServerError(e);
        }
        if(mediaObject == null) {
            throw new BadRequest("No media for id " + id);
        }
        return mediaObject;
    }

    @Override
    public MediaResult listMembers(String id, String profile, Long offset, Integer max, boolean mock) {
        return findMembers(null, id, profile, offset, max, mock).asResult();
    }

    @Override
    public MediaSearchResult findMembers(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.findMembers(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findMembers(id, profile, form, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public ProgramResult listEpisodes(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listEpisodes(id, profile, offset, max, true);
        }
        return findEpisodes(null, id, profile, offset, max, mock).asResult();
    }

    @Override
    public ProgramSearchResult findEpisodes(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.findEpisodes(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findEpisodes(id, profile, form, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }

    }

    @Override
    public MediaResult listDescendants(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listDescendants(id, profile, offset, max, true);
        }
        return findDescendants(null, id, profile, offset, max, mock).asResult();
    }

    @Override
    public MediaSearchResult findDescendants(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.findDescendants(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findDescendants(id, profile, form, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }


    }

    @Override
    public MediaResult listRelated(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listRelated(id, profile, offset, max, true);
        }
        return findRelated(null, id, profile, offset, max, mock).asResult();
    }

    @Override
    public MediaSearchResult findRelated(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.findRelated(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findRelated(id, profile, form, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }
    }
}
