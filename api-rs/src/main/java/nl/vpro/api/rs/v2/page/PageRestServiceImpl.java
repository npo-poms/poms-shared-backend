package nl.vpro.api.rs.v2.page;

import javax.annotation.PostConstruct;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wordnik.swagger.annotations.*;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.Error;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageService;
import nl.vpro.swagger.SwaggerApplication;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
@Api(value = PageRestService.PATH, position = 2)
public class PageRestServiceImpl implements PageRestService {
    private static long listSizes = 100l;

    private static final String DEMO_FORM = "{\n" +
        "    \"facets\": {\n" +
        "        \"sortDates\": {\n" +
        "            \"presets\": [\n" +
        "                \"LAST_WEEK\", \"LAST_YEAR\", \"BEFORE_LAST_YEAR\"\n" +
        "            ]\n" +
        "        }\n" +
        "    },\n" +
        "    \"highlight\": true,\n" +
        "    \"searches\": {\n" +
        "        \"text\": \"Tegenlicht\",\n" +
        "        \"pageTypes\": [\n" +
        "            \"ARTICLE\"\n" +
        "        ]\n" +
        "    }\n" +
        "}\n";

    private final PageService pageService;

    @Value("${api.pages.expose}")
    private boolean expose;

    @Autowired
    PageRestServiceImpl(PageService pageService) {
        this.pageService = pageService;
    }

    @PostConstruct
    private void init() {
        if(expose) {
            SwaggerApplication.inject(this);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Get all pages",
        notes = "Get all pages filtered on an optional profile",
        position = 0
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public Response list(
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return Response.ok(pageService.find(null, profile, offset, max).asResult()).build();
        } catch(Exception e) {
            return Response.ok(new Error(200, e)).status(200).build();
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find pages",
        notes = "Find pages by posting a search form. Results are filtered on an optional profile",
        position = 1
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public Response find(
        @ApiParam(value = "Search form", required = false, defaultValue = DEMO_FORM) PageForm form,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return Response.ok(pageService.find(form, profile, offset, max)).build();

        } catch(Exception e) {
            return Response.ok(new Error(500, e)).status(500).build();
        }
    }
}
