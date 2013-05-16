package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.MediaForm;

import java.util.Arrays;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class MockMediaRestService implements MediaRestService {
	@Override
	public PagedResult<MediaObject> list(String profile, Integer offset, Integer limit,  boolean mock) {
		return new PagedResult<MediaObject>(
				Arrays.asList(
						MediaBuilder.program().build(),
						MediaBuilder.program().build(),
						MediaBuilder.program().build()
				), offset, 100);
	}

	@Override
	public PagedResult<MediaObject> search(MediaForm form, String profile, Integer offset, Integer limit, boolean mock) {
		return new PagedResult<MediaObject>(
				Arrays.asList(
						MediaBuilder.program().build(),
						MediaBuilder.program().build(),
						MediaBuilder.program().build()
				), offset, 100);
	}

	@Override
	public MediaObject get(String id, boolean mock) {
		return MediaTestDataBuilder.program().build();

	}

	@Override
	public PagedResult<MediaObject> getMembers(String id, boolean mock) {
		return new PagedResult<>();
	}

	@Override
	public PagedResult<Program> getEpisodes(String id, boolean mock) {
		return new PagedResult<>();
	}

	@Override
	public PagedResult<Segment> getSegments(String id, boolean mock) {
		return new PagedResult<>();
	}
}
