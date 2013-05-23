/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.api.rs.v2.exception.BadRequest;
import nl.vpro.api.rs.v2.exception.ServerError;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl implements MediaRestService {

    private final MockMediaRestService mocks = new MockMediaRestService();

    private final MediaService mediaService;

    @Autowired
    public MediaRestServiceImpl(MediaService mediaService) {
        this.mediaService = mediaService;

    }

    @Override
    public Result<MediaObject> list(
            String profile,
            Long offset,
            Integer max,
            boolean mock) {
        if(mock) {
            return mocks.list(profile, offset, max, true);
        }
        try {
            return mediaService.find(profile, null, offset, max).asResult();
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public SearchResult<MediaObject> find(
        MediaForm form,
        String profile,
        Long offset,
        Integer max,
        boolean mock) {
        if(mock) {
            return mocks.find(form, profile, offset, max, true);
        }
        try {
            return mediaService.find(profile, form, offset, max);
        } catch (Exception e) {
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
        if (mediaObject == null) {
            throw new BadRequest("No media for id " + id);
        }
        return mediaObject;
    }

    @Override
    public Result<MediaObject> listMembers(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listMembers(id, profile, offset, max, true);
        }
        try {
            return mediaService.findMembers(id, profile, null, offset, max).asResult();
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public SearchResult<MediaObject> findMembers(MediaForm form, String id, String profile, Long  offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findMembers(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findMembers(id, profile, form, offset, max);
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public Result<Program> listEpisodes(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listEpisodes(id, profile, offset, max, true);
        }
        try {
            return mediaService.findEpisodes(id, profile, null, offset, max).asResult();
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public SearchResult<Program> findEpisodes(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findEpisodes(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findEpisodes(id, profile, form, offset, max);
        } catch (Exception e) {
            throw new ServerError(e);
        }

    }

    @Override
    public Result<MediaObject> listDescendants(String id, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listDescendants(id, profile, offset, max, true);
        }
        try {
            return mediaService.findDescendants(id, profile, null, offset, max).asResult();
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public SearchResult<MediaObject> findDescendants(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findDescendants(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findDescendants(id, profile, form, offset, max);
        } catch (Exception e) {
            throw new ServerError(e);
        }


    }

    @Override
    public Result<MediaObject> listRelated(String id, String profile, Long offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.listRelated(id, profile, offset, max, true);
        }
        try {
            return mediaService.findRelated(id, profile, null, offset, max).asResult();
        } catch (Exception e) {
            throw new ServerError(e);
        }


    }

    @Override
    public SearchResult<MediaObject> findRelated(MediaForm form, String id, String profile, Long offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findRelated(form, id, profile, offset, max, true);
        }
        try {
            return mediaService.findRelated(id, profile, form, offset, max);
        } catch (Exception e) {
            throw new ServerError(e);
        }
    }
}
