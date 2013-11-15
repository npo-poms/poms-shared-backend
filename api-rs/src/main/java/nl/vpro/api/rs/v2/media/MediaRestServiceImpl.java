/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wordnik.swagger.annotations.*;

import nl.vpro.api.rs.v2.Responses;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.api.transfer.MediaTransferResult;
import nl.vpro.domain.api.transfer.MediaTransferSearchResult;
import nl.vpro.domain.api.transfer.ProgramTransferResult;
import nl.vpro.domain.api.transfer.ProgramTransferSearchResult;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.jackson.ObjectMapper;
import nl.vpro.swagger.SwaggerApplication;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.PropertySelection;

import static nl.vpro.api.rs.v2.Util.exception;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
@Api(value = MediaRestService.PATH, description = "The media API")
public class MediaRestServiceImpl implements MediaRestService {

    private static final String DEFAULT_FORM = "{\n" +
        "    \"facets\": {\n" +
        "        \"sortDates\": {\n" +
        "            \"presets\": [\n" +
        "                \"LAST_WEEK\", \"LAST_YEAR\", \"BEFORE_LAST_YEAR\"\n" +
        "            ]\n" +
        "        }\n" +
        "    },\n" +
        "    \"highlight\": true, \n" +
        "    \"searches\": {\n" +
        "        \"descendantOf\": [\n" +
        "            {\n" +
        "                \"value\": \"urn:vpro:media:group:72709\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }\n" +
        "}";

    private final MockMediaRestService mocks = new MockMediaRestService();

    private final MediaService mediaService;

    @Value("${api.media.expose}")
    private boolean expose;

    @Autowired
    public MediaRestServiceImpl(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostConstruct
    private void init() {
        if(expose) {
            SwaggerApplication.inject(this);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Get all media",
        notes = "Get all media filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Override
    public Response list(
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.list(profile, properties, offset, max, true);
        }

        try {
            MediaSearchResult searchResult = mediaService.find(profile, null, offset, max);
            return filteredAsMediaResult(searchResult, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find media objects",
        notes = "Find media object by posting a search form. Results are filtered on an optional profile")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Override
    public Response find(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.find(form, profile, properties, offset, max, true);
        }
        try {
            MediaSearchResult result = mediaService.find(profile, form, offset, max);
            return filteredAsMediaSearchResult(result, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Load media",
        notes = "Load media by id. The media id is the Media object's URN")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}")
    @Override
    public Response load(
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.load(id, properties, true);
        }

        try {
            MediaObject mediaObject = mediaService.load(id);
            if(mediaObject == null) {
                return Responses.mediaNotFound(id);
            }

            if(properties == null) {
                return Response.ok(mediaObject).build();
            }
            return Response.ok(MediaTransfer.create(mediaObject, new PropertySelection(properties))).build();
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Load members",
        notes = "Load all members of a certain media object. Often the media object would be a group.")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/members")
    @Override
    public Response listMembers(
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {

        try {
            MediaSearchResult searchResult = mediaService.findMembers(id, profile, null, offset, max);
            return filteredAsMediaResult(searchResult, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find members",
        notes = "Search in the members of a media object"
    )
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/members")
    @Override
    public Response findMembers(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.findMembers(form, id, profile, properties, offset, max, true);
        }
        try {
            MediaSearchResult members = mediaService.findMembers(id, profile, form, offset, max);
            return filteredAsMediaSearchResult(members, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "List episodes",
        notes = "List the episodes of a media group")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/episodes")
    @Override
    public Response listEpisodes(
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.listEpisodes(id, profile, properties, offset, max, true);
        }

        try {
            ProgramSearchResult episodes = mediaService.findEpisodes(id, profile, null, offset, max);
            return filteredAsProgramResult(episodes, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find eposides",
        notes = "Search in the episodes of the media group")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/episodes")
    @Override
    public Response findEpisodes(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile, @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.findEpisodes(form, id, profile, properties, offset, max, true);
        }

        try {
            ProgramSearchResult episodes = mediaService.findEpisodes(id, profile, form, offset, max);
            return filteredAsProgramSearchResult(episodes, properties);
        } catch(Exception e) {
            throw exception(e);
        }

    }

    @ApiOperation(httpMethod = "get",
        value = "List descendants",
        notes = "List all descendants of a certain media group. That means all its members and all the members of those and so on")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/descendants")
    @Override
    public Response listDescendants(
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.listDescendants(id, profile, properties, offset, max, true);
        }

        try {
            MediaSearchResult descendants = mediaService.findDescendants(id, profile, null, offset, max);
            return filteredAsMediaSearchResult(descendants, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find descendants",
        notes = "Search in all descendants of a certain media group")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/descendants")
    @Override
    public Response findDescendants(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.findDescendants(form, id, profile, properties, offset, max, true);
        }
        try {
            MediaSearchResult descendants = mediaService.findDescendants(id, profile, form, offset, max);
            return filteredAsMediaSearchResult(descendants, properties);
        } catch(Exception e) {
            throw exception(e);
        }


    }

    @ApiOperation(httpMethod = "get",
        value = "List related",
        notes = "List all media objects that are related to another one.")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/related")
    @Override
    public Response listRelated(
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.listRelated(id, profile, properties, offset, max, true);
        }
        try {
            MediaSearchResult related = mediaService.findRelated(id, profile, null, offset, max);
            return filteredAsMediaResult(related, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find related",
        notes = "Search in all media objects that are related to another one."
    )
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @Path("/{id}/related")
    @Override
    public Response findRelated(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max,
        @ApiParam @QueryParam("mock") @DefaultValue("false") boolean mock
    ) {
        if(mock) {
            return mocks.findRelated(form, id, profile, properties, offset, max, true);
        }
        try {
            MediaSearchResult related = mediaService.findRelated(id, profile, form, offset, max);
            return filteredAsMediaSearchResult(related, properties);
        } catch(Exception e) {
            throw exception(e);
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Retrieve changes",
        notes = "Retrieve all media changes since a certain update sequence.\n" +
            "By submitting an optional profile argument only changes for this argument are emitted.")
    @ApiErrors(value = {@ApiError(code = 400, reason = "Bad request"), @ApiError(code = 500, reason = "Server error")})
    @GET
    @Path("/changes")
    @Override
    public Response changes(
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(required = false) @QueryParam("since") Long since,
        @ApiParam(defaultValue = "asc", required = false) @QueryParam("order") @DefaultValue("asc") String sOrder,
        @ApiParam(defaultValue = "10", required = false) @QueryParam("max") Integer max,
        @Context HttpServletRequest request,
        @Context HttpServletResponse response) throws IOException {

        Order order = Order.valueOf(sOrder.toUpperCase());

        if(true) { // TODO xml / mock
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"changes\": [\n");
            Iterator<Change> changes = mediaService.changes(profile, since, order, max);
            boolean first = true;
            while(changes.hasNext()) {
                if(!first) {
                    writer.write(",\n");
                } else {
                    first = false;
                }
                Change change = changes.next();
                writer.write(ObjectMapper.INSTANCE.writeValueAsString(change));
            }
            writer.write("\n]}");
            writer.close();
        }
        return null;
    }

    private Response filteredAsMediaResult(MediaSearchResult searchResult, String properties) {
        if(properties == null) {
            return Response.ok(searchResult.asResult()).build();
        }

        return Response.ok(MediaTransferResult.create(searchResult.asResult(), new PropertySelection(properties))).build();
    }

    private Response filteredAsProgramResult(ProgramSearchResult searchResult, String properties) {
        if(properties == null) {
            return Response.ok(searchResult.asResult()).build();
        }

        return Response.ok(ProgramTransferResult.create(searchResult.asResult(), new PropertySelection(properties))).build();
    }

    private Response filteredAsMediaSearchResult(MediaSearchResult searchResult, String properties) {
        if(properties == null) {
            return Response.ok(searchResult).build();
        }

        return Response.ok(MediaTransferSearchResult.create(searchResult, new PropertySelection(properties))).build();
    }

    private Response filteredAsProgramSearchResult(ProgramSearchResult searchResult, String properties) {
        if(properties == null) {
            return Response.ok(searchResult).build();
        }

        return Response.ok(ProgramTransferSearchResult.create(searchResult, new PropertySelection(properties))).build();
    }
}
