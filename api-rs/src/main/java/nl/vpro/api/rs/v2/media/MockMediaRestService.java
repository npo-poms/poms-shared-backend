package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.*;
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



    @Override
    public MediaObject get(String id, boolean mock) {
        return build(id.hashCode());

    }

    @Override
    public PagedResult<MediaObject> getMembers(String id, Integer offset, Integer limit, boolean mock) {
        return new PagedResult<>(mockList(listSizes, offset, limit), offset, listSizes);
    }

    @Override
    public PagedResult<Program> getEpisodes(String id, Integer offset, Integer limit, boolean mock) {
        return new PagedResult<>(mockEpisodes(listSizes, offset, limit), offset, listSizes);
    }

    @Override
    public PagedResult<Segment> getSegments(String id, Integer offset, Integer limit, boolean mock) {
        return new PagedResult<>(mockSegments(listSizes, offset, limit), offset, listSizes);
    }

    protected List<MediaObject> mockList(int total, int offset, int limit) {
        int numberOfResults = Math.min(total - offset, limit);
        List<MediaObject> result = new ArrayList<>();
        for (int i = 0; i < numberOfResults; i++) {
            result.add(build(i));
        }
        return result;
    }

    protected MediaObject build(int hash) {
        switch (hash % 3) {
            case 1:
                return MediaTestDataBuilder.group().constrained().build();
            case 2:
                return MediaTestDataBuilder.segment().constrained().build();
            default:
                return MediaTestDataBuilder.program().constrained().build();

        }
    }

    protected List<Program> mockEpisodes(int total, int offset, int limit) {
        int numberOfResults = Math.min(total - offset, limit);
        List<Program> result = new ArrayList<>();
        for (int i = 0; i < numberOfResults; i++) {
            result.add(MediaTestDataBuilder.program().constrained().type(ProgramType.BROADCAST).build());
        }
        return result;
    }

    protected List<Segment> mockSegments(int total, int offset, int limit) {
        int numberOfResults = Math.min(total - offset, limit);
        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < numberOfResults; i++) {
            result.add(MediaTestDataBuilder.segment().constrained().build());
        }
        return result;
    }
}
