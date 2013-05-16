/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.stereotype.Service;

import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.MediaObject;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl<T extends MediaObject> implements MediaRestService<T> {

    @Override
    public T get(@PathParam("id") String id) {
        return (T)MediaBuilder.program().build();
    }
}
