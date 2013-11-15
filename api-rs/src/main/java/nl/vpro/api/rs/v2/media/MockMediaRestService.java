package nl.vpro.api.rs.v2.media;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import nl.vpro.domain.api.*;
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
    private static long listSizes = 100l;

    @Override
    public Response list(String profile, String properties, Long offset, Integer max, boolean mock) {
        List<MediaObject> list = mockList(listSizes, offset, max);
        MediaResult result = new MediaResult(list, offset, max, listSizes);
        return Response.ok(result).build();
    }

    @Override
    public Response find(MediaForm form, String profile, String properties, Long offset, Integer max, boolean mock) {
        List<SearchResultItem<? extends MediaObject>> list = mockSearchItems(mockList(listSizes, offset, max));
        MediaFacetsResult facetsResult = new MediaFacetsResult();
        facetsResult.setBroadcasters(Arrays.asList(new TermFacetResultItem("vpro", 100)));
        facetsResult.setSortDates(Arrays.asList(new DateFacetResultItem("LAST_YEAR", new Date(0), new Date(), 100)));
        return Response.ok(new MediaSearchResult(list, facetsResult, offset, max, listSizes)).build();
    }

    @Override
    public Response load(String id, String properties, boolean mock) {
        return Response.ok(build(id)).build();
    }

    @Override
    public Response listMembers(String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaResult(mockList(listSizes, offset, max), offset, max, listSizes)).build();
    }

    @Override
    public Response findMembers(MediaForm form, String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaSearchResult(mockSearchItems(mockList(listSizes, offset, max)), offset, max, listSizes)).build();
    }

    @Override
    public Response listEpisodes(String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new ProgramSearchResult(mockSearchItems(mockEpisodes(listSizes, offset, max)), offset, max, listSizes).asResult()).build();
    }

    @Override
    public Response findEpisodes(MediaForm form, String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new ProgramSearchResult(mockSearchItems(mockEpisodes(listSizes, offset, max)), offset, max, listSizes)).build();

    }

    @Override
    public Response listDescendants(String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaResult(mockList(listSizes, offset, max), offset, max, listSizes)).build();
    }

    @Override
    public Response findDescendants(MediaForm form, String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaSearchResult(mockSearchItems(mockList(listSizes, offset, max)), offset, max, listSizes)).build();
    }

    @Override
    public Response listRelated(String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaResult(mockList(listSizes, offset, max), offset, max, listSizes)).build();
    }

    @Override
    public Response findRelated(MediaForm form, String id, String profile, String properties, Long offset, Integer max, boolean mock) {
        return Response.ok(new MediaSearchResult(mockSearchItems(mockList(listSizes, offset, max)), offset, max, listSizes)).build();
    }

    @Override
    public Response changes(String profile, Long since, @DefaultValue("asc") String order, Integer max, @Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
        // TODO
        return null;
    }

    protected <T extends MediaObject> List<SearchResultItem<? extends T>> mockSearchItems(final List<T> list) {
        return new AbstractList<SearchResultItem<? extends T>>() {

            @Override
            public SearchResultItem<T> get(int index) {
                SearchResultItem<T> result = new SearchResultItem<>(list.get(index));
                result.setScore(0.5f);
                result.setHighlights(Arrays.asList(new HighLight("foo", "bla <em>foo</em> bloe", "blie <em>foo</em> blo")));
                return result;
            }

            @Override
            public int size() {
                return list.size();
            }
        };

    }

    protected List<MediaObject> mockList(long total, long offset, int max) {
        long numberOfResults = Math.min(total - offset, max);
        List<MediaObject> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(build("" + i));
        }
        return result;
    }

    protected MediaObject build(String id) {
        if (id.contains("program")) return MediaTestDataBuilder.program().constrained().build();
        if (id.contains("group")) return MediaTestDataBuilder.group().constrained().build();
        if (id.contains("segment")) return MediaTestDataBuilder.segment().constrained().build();
        switch(id.hashCode() % 3) {
            case 1:
                return MediaTestDataBuilder.group().constrained().build();
            case 2:
                return MediaTestDataBuilder.segment().constrained().build();
            default:
                return MediaTestDataBuilder.program().constrained().build();

        }
    }

    protected List<Program> mockEpisodes(long total, long offset, int max) {
        long numberOfResults = Math.min(total - offset, max);
        List<Program> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(MediaTestDataBuilder.program().constrained().type(ProgramType.BROADCAST).build());
        }
        return result;
    }
}
