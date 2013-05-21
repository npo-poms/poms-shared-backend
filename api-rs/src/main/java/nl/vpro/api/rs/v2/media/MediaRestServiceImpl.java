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
    public Result<MediaObject> list(
        String profile,
        Integer offset,
        Integer limit,
        boolean mock) {
        if(mock) {
            return mocks.list(profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @Override
    public Result<MediaObject> search(
        MediaForm form,
        String profile,
        Integer offset,
        Integer limit,
        boolean mock) {
        if(mock) {
            return mocks.search(form, profile, offset, limit, true);
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MediaObject get(String id, boolean mock) {
        if(mock) {
            return mocks.get(id, true);
        }

        throw new BadRequest("No media for id " + id);
    }

    @Override
    public Result<MediaObject> listMembers(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.listMembers(id, profile, offset, limit, true);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Result<MediaObject> listMembers(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<Program> listEpisodes(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.listEpisodes(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result<Program> listEpisodes(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> listDescendants(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if(mock) {
            return mocks.listDescendants(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result<MediaObject> listDescendants(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> listRelated(String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }

    @Override
    public Result<MediaObject> listRelated(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return null;
    }
}
