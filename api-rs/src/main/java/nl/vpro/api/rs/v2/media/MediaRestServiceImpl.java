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

import javax.ws.rs.DefaultValue;
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

	MockMediaRestService mocks = new MockMediaRestService();

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

            return new PagedResult<MediaObject>(
                    Arrays.asList(
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build(),
                            MediaBuilder.program().build()
                    ), offset, 100);

        } else {
            return new PagedResult<>();
        }

    }

    @Override
    public MediaObject get(@PathParam("id") String id, @QueryParam("mock") @DefaultValue("false") boolean mock) {
        if (mock) {
            return MediaBuilder.program().build();
        } else {
            // klinkt logisch, maar dat geeft een 404 dus...
			throw new NotFoundException("bla");
		}
    }

	@Override
	public PagedResult<MediaObject> getMembers(@PathParam("id") String id, @DefaultValue("false") boolean mock) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public PagedResult<Program> getEpisodes(@PathParam("id") String id, @DefaultValue("false") boolean mock) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public PagedResult<Segment> getSegments(@PathParam("id") String id, @DefaultValue("false") boolean mock) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
