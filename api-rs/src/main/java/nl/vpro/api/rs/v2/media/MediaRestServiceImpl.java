/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

import nl.vpro.api.rs.v2.PagedResult;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaSearchResult;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl<T extends MediaObject> implements MediaRestService<T> {

    @Override
    public PagedResult<MediaObject> list(@DefaultValue("false") boolean mock) {
        return null;
    }

    @Override
    public PagedResult<MediaObject> search(MediaForm form, @DefaultValue("false") boolean mock) {
        return null;
    }

    @Override
    public MediaObject get(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock) {
        return MediaBuilder.program().build();
    }
}
