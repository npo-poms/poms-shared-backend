/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import org.jboss.resteasy.spi.NotFoundException;
import org.springframework.stereotype.Service;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.search.MediaForm;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl  implements MediaRestService  {

    private final MockMediaRestService mocks = new MockMediaRestService();

    @Override
    public Result<MediaObject> list(
            String profile,
            Integer offset,
            Integer limit,
            boolean mock) {
        if (mock) {
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
        if (mock) {
            return mocks.search(form, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException("TODO");
        }

    }

    @Override
    public MediaObject get(String id, boolean mock) {
        if (mock) {
            return mocks.get(id, true);
        } else {
            // klinkt logisch, maar dat geeft een 404 dus...
            throw new NotFoundException("bla");
        }
    }

    @Override
    public Result<MediaObject> listMembers(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if (mock) {
            return mocks.listMembers(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public Result<Program> listEpisodes(String id, String profile, Integer offset, Integer limit, boolean mock) {
        if (mock) {
            return mocks.listEpisodes(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public Result<Segment> listDescendants(String id, String profile,Integer offset, Integer limit, boolean mock) {
        if (mock) {
            return mocks.listDescendants(id, profile, offset, limit, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
