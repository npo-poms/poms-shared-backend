/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.search.MediaForm;
import org.jboss.resteasy.spi.NotFoundException;
import org.springframework.stereotype.Service;

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
    public PagedResult<MediaObject> list(
            String profile,
            Integer offset,
            Integer limit,
            boolean mock) {
        if (mock) {
            return mocks.list(profile, offset, limit, true);
        } else {
            return new PagedResult<>();
        }
    }

    @Override
    public PagedResult<MediaObject> search(
            MediaForm form,
            String profile,
            Integer offset,
            Integer limit,
            boolean mock) {
        if (mock) {
            return mocks.search(form, profile, offset, limit, true);
        } else {
            return new PagedResult<>();
        }

    }

    @Override
    public MediaObject get(String id, boolean mock) {
        if (mock) {
            return MediaBuilder.program().build();
        } else {
            // klinkt logisch, maar dat geeft een 404 dus...
            throw new NotFoundException("bla");
        }
    }

    @Override
    public PagedResult<MediaObject> getMembers(String id, boolean mock) {
        if (mock) {
            return mocks.getMembers(id, true);
        } else {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public PagedResult<Program> getEpisodes(String id, boolean mock) {
        if (mock) {
            return mocks.getEpisodes(id, true);
        } else {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public PagedResult<Segment> getSegments(String id, boolean mock) {
        if (mock) {
            return mocks.getSegments(id, true);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
