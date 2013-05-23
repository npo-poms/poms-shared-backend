/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.api.rs.v2.exception.BadRequest;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
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

    @Override
    public Result<MediaObject> list(
            String profile,
            Integer offset,
            Integer max,
            boolean mock) {
        if(mock) {
            return mocks.list(profile, offset, max, true);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @Override
    public SearchResult<MediaObject> find(
        MediaForm form,
        String profile,
        Integer offset,
        Integer max,
        boolean mock) {
        if(mock) {
            return mocks.find(form, profile, offset, max, true);
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
    public Result<MediaObject> listMembers(String id, String profile, Integer offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listMembers(id, profile, offset, max, true);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public SearchResult<MediaObject> findMembers(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findMembers(form, id, profile, offset, max, true);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Program> listEpisodes(String id, String profile, Integer offset, Integer max, boolean mock) {
        if(mock) {
            return mocks.listEpisodes(id, profile, offset, max, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public SearchResult<Program> findEpisodes(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
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
    public SearchResult<MediaObject> findDescendants(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findDescendants(form, id, profile, offset, max, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result<MediaObject> listRelated(String id, String profile, Integer offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.listRelated(id, profile, offset, max, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public SearchResult<MediaObject> findRelated(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        if (mock) {
            return mocks.findRelated(form, id, profile, offset, max, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
