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
    public Result<MediaObject> list(String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> search(MediaForm form, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public MediaObject get(String id, boolean mock) {
        return build(id.hashCode());
    }

    @Override
    public Result<MediaObject> listMembers(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return listMembers(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> listMembers(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<Program> listEpisodes(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockEpisodes(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<Program> listEpisodes(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return listEpisodes(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> listDescendants(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> listDescendants(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return listDescendants(id, profile, offset, max, mock);
    }

    @Override
    public Result<MediaObject> listRelated(String id, String profile, Integer offset, Integer max, boolean mock) {
        return new Result<>(mockList(listSizes, offset, max), offset, listSizes);
    }

    @Override
    public Result<MediaObject> listRelated(MediaForm form, String id, String profile, Integer offset, Integer max, boolean mock) {
        return listRelated(id, profile, offset, max, mock);
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
