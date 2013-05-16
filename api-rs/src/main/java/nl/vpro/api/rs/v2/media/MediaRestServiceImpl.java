/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.search.MediaForm;
import org.springframework.stereotype.Service;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Arrays;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaRestServiceImpl  implements MediaRestService  {

    @Override
    public PagedResult<MediaObject> list(@DefaultValue("false") boolean mock) {
        if (mock) {
            return new PagedResult<MediaObject>(
                    Arrays.asList(
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build()
                    ), 10);

        } else {
            return new PagedResult<MediaObject>();
        }
    }

    @Override
    public PagedResult<MediaObject> search(MediaForm form, @DefaultValue("false") boolean mock) {
        if (mock) {
            return new PagedResult<MediaObject>(
                    Arrays.asList(
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build()
                    ), 10);

        } else {
            return new PagedResult<MediaObject>();
        }

    }

    @Override
    public MediaObject get(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock) {
        if (mock) {
            return MediaBuilder.program().build();
        } else {
            // klinkt logisch, maar dat geeft een 404 dus...
            throw new NotFoundException();
        }
    }
}
