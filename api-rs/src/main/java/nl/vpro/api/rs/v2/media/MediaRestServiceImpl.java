/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import org.springframework.stereotype.Service;

import nl.vpro.api.rs.v2.exception.BadRequest;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl implements MediaRestService {

    private final MockMediaRestService mocks = new MockMediaRestService();

    @Override
    public Result<MediaObject> find(
        String profile,
        Integer offset,
        Integer limit,
        boolean mock) {
        if(mock) {
            return mocks.find(profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @Override
    public Result<MediaObject> find(
        MediaForm form,
        String profile,
        Integer offset,
        Integer limit,
        boolean mock) {
        if(mock) {
            return mocks.find(form, profile, offset, limit, true);
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MediaObject load(String id, boolean mock) {
        if(mock) {
            return mocks.load(id, true);
        }

        throw new BadRequest("No media for id " + id);
    }

    @Override
    public Result<MediaObject> findMembers(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.findMembers(id, profile, offset, limit, true);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Result<MediaObject> findMembers(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<Program> findEpisodes(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.findEpisodes(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result<Program> findEpisodes(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> findDescendants(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.findDescendants(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result<MediaObject> findDescendants(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> findRelated(String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> findRelated(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }
}
