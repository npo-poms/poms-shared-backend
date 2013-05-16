package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.search.MediaForm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
class MockMediaRestService implements MediaRestService {
	private static int listSizes = 100;
	@Override
	public PagedResult<MediaObject> list(String profile, Integer offset, Integer limit,  boolean mock) {
		return new PagedResult<>(mockList(listSizes, offset, limit), offset, listSizes);
	}

	@Override
	public PagedResult<MediaObject> search(MediaForm form, String profile, Integer offset, Integer limit, boolean mock) {
		return new PagedResult<>(mockList(listSizes, offset, limit), offset, listSizes);

	}

	protected List<MediaObject> mockList(int total, int offset, int limit) {
		int numberOfResults = Math.min(total - offset, limit);
		List<MediaObject> result = new ArrayList<>();
		for (int i = 0; i < numberOfResults; i++) {
			result.add(MediaTestDataBuilder.program().build());
		}
		return result;
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
