package nl.vpro.api.rs.v2.media;

import java.util.ArrayList;
import java.util.List;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.ProgramType;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
class MockMediaRestService implements MediaRestService {
    private static int listSizes = 100;

    @Override
    public Result<MediaObject> find(String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> find(MediaForm form, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public MediaObject load(String id, boolean mock) {
        return build(id.hashCode());
    }

    @Override
    public Result<MediaObject> findMembers(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return findMembers(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> findMembers(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<Program> findEpisodes(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockEpisodes(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<Program> findEpisodes(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return findEpisodes(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> findDescendants(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> findDescendants(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return findDescendants(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> findRelated(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> findRelated(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return findRelated(id, profile, offset, max, mock);
    }

    protected List<MediaObject> mockList(int total, int offset, int max) {
        int numberOfResults = Math.min(total - offset, max);
        List<MediaObject> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(build(i));
        }
        return result;
    }

    protected MediaObject build(int hash) {
        switch(hash % 3) {
            case 1:
                return MediaTestDataBuilder.group().constrained().build();
            case 2:
                return MediaTestDataBuilder.segment().constrained().build();
            default:
                return MediaTestDataBuilder.program().constrained().build();

        }
    }

    protected List<Program> mockEpisodes(int total, int offset, int max) {
        int numberOfResults = Math.min(total - offset, max);
        List<Program> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(MediaTestDataBuilder.program().constrained().type(ProgramType.BROADCAST).build());
        }
        return result;
    }
}
