package nl.vpro.api.rs.v2.page;

import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wordnik.swagger.annotations.*;

import nl.vpro.api.rs.v2.exception.ServerError;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageService;
import nl.vpro.domain.page.Page;
import nl.vpro.domain.page.PageBuilder;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
@Path(PageRestService.PATH)
@Api(value = PageRestService.PATH, description = "The pages API")
public class PageRestServiceImpl implements PageRestService {
    private static long listSizes = 100l;

    private final PageService pageService;

    @Autowired
    PageRestServiceImpl(PageService pageService) {
        this.pageService = pageService;
    }

    @GET
    @ApiOperation(value = "Get all pages", notes = "Get all pages filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 404, reason = "Server error")})
    @Override
    public PageResult list(String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return new PageResult(mockList(listSizes, offset, max), offset, max, listSizes);
        }
         return find(null, profile, offset, max, mock).asResult();
    }

    @POST
    @ApiOperation(value = "Find pages", notes = "Find pages by posting a search form. Results are filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 404, reason = "Server error")})
    @Override
    public PageSearchResult find(
        @ApiParam(value = "Search form", required = false, defaultValue = "{\n" +
            "   \"highlight\" : true,\n" +
            "   \"facets\" :\n" +
            "      {\n" +
            "      },\n" +
            "   \"searches\" :\n" +
            "      {\n" +
            "         \"types\" :\n" +
            "            [\n" +
            "               \"PRODUCT\"\n" +
            "            ]\n" +
            "      }\n" +
            "}") PageForm form,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            PageSearchResult result = new PageSearchResult(mockSearchItems(mockList(listSizes, offset, max)), offset, max, listSizes);
            result.setPublisherFacetResult(Arrays.asList(new TermFacetResultItem("kro", 10)));
            return result;
        }
        try {
            return pageService.find(form, profile, offset, max);
        } catch(Exception e) {
            throw new ServerError(e);
        }
    }

    @Override
    public Page load(String id, boolean mock) {
        if(mock) {
            return mockPage();
        }
        return pageService.load(id);
    }

    protected List<? extends Page> mockList(long total, long offset, int limit) {
        long numberOfResults = Math.min(total - offset, limit);
        List<Page> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(mockPage());
        }
        return result;
    }


    protected List<SearchResultItem<? extends Page>> mockSearchItems(final List<? extends Page> list) {
        return new AbstractList<SearchResultItem<? extends Page>>() {

            @Override
            public SearchResultItem<Page> get(int index) {
                SearchResultItem<Page> result = new SearchResultItem<>(list.get(index));
                result.setScore(0.5f);
                //
                return result;
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }


    protected Page mockPage() {
        try {
            return PageBuilder.page().id("4b748d32-8006-4f0a-8aac-6d8d5c89a847").example().build();
        } catch(URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
