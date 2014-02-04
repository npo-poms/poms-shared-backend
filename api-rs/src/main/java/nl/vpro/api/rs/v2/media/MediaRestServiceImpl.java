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

import nl.vpro.api.rs.v2.exception.Exceptions;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.bind.Jackson2Mapper;
import nl.vpro.swagger.SwaggerApplication;

import static nl.vpro.api.rs.v2.exception.Exceptions.serverError;

/**
 * See https://jira.vpro.nl/browse/API-92
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
@Api(value = MediaRestService.PATH, position = 0)
public class MediaRestServiceImpl implements MediaRestService {

    private static final String DEFAULT_FORM = "{\n" +
        "    \"searches\" : {\n" +
        "        \"text\" : {\n" +
        "                \"value\" : \"Argos\"\n" +
        "        }\n" +
        "    }\n" +
        "}";

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
        notes = "Get all media filtered on an optional profile",
        position = 0
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public MediaResult list(
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.list(offset, max);
//            return filteredMediaResult(searchResult, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Find media",
        notes = "Find media object by posting a search form. Results are filtered on an optional profile",
        position = 1
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public MediaSearchResult find(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {

        try {
            return mediaService.find(profile, form, offset, max);
//            return filteredAsMediaSearchResult(result, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Load entity",
        notes = "Load media by its id",
        position = 2
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}")
    @Override
    public MediaObject load(
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties
    ) {
        MediaObject mediaObject = handleNotFound(id);

/*
            if(properties == null) {
                return Response.ok(mediaObject).build();
            }
            return Response.ok(MediaTransfer.create(mediaObject, new PropertySelection(properties))).build();
*/
        return mediaObject;
    }

    @ApiOperation(httpMethod = "get",
        value = "Load members",
        notes = "Load all members of a certain media object. Often the media object would be a group.",
        position = 3
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/members")
    @Override
    public MediaResult listMembers(
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        handleNotFound(id);

        try {
            return mediaService.listMembers(id, offset, max);
//            return filteredMediaResult(result, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Filter members",
        notes = "Search in the members of a media object (note: filtering purges all duplicate elements)",
        position = 4
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/members")
    @Override
    public MediaSearchResult findMembers(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.findMembers(id, profile, form, offset, max);
//            return filteredAsMediaSearchResult(members, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "List episodes",
        notes = "List the episodes of a media group",
        position = 5
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/episodes")
    @Override
    public ProgramResult listEpisodes(
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.listEpisodes(id, offset, max);
//            return filteredProgramResult(episodes, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Filter eposides",
        notes = "Search in the episodes of the media group (note: filtering purges all duplicate elements)",
        position = 6
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/episodes")
    @Override
    public ProgramSearchResult findEpisodes(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile, @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.findEpisodes(id, profile, form, offset, max);
//            return filteredAsProgramSearchResult(episodes, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }

    }

    @ApiOperation(httpMethod = "get",
        value = "List descendants",
        notes = "List all descendants of a certain media group. That means all its members and all the members of those and so on",
        position = 7
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/descendants")
    @Override
    public MediaResult listDescendants(
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {

            return mediaService.listDescendants(id, offset, max);
//            return filteredMediaResult(descendants, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Filter descendants",
        notes = "Search in all descendants of a certain media group (note: filtering purges all duplicate elements)",
        position = 8
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/descendants")
    @Override
    public MediaSearchResult findDescendants(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.findDescendants(id, profile, form, offset, max);
//            return filteredAsMediaSearchResult(descendants, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "List related",
        notes = "List all media objects that are related to another one.",
        position = 9
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/related")
    @Override
    public MediaResult listRelated(
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.listRelated(id, offset, max);
//            return filteredMediaResult(related, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "post",
        value = "Filter related",
        notes = "Search in all media objects that are related to another one (note: filtering purges all duplicate elements)",
        position = 10
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Path("/{id}/related")
    @Override
    public MediaSearchResult findRelated(
        @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
        @ApiParam(required = true, defaultValue = "AVRO_1656037") @PathParam("id") String id,
        @ApiParam(required = false) @QueryParam("profile") String profile,
        @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            return mediaService.findRelated(id, profile, form, offset, max);
//            return filteredAsMediaSearchResult(related, properties);
        } catch(Exception e) {
            throw serverError(e.getMessage());
        }
    }

    @ApiOperation(httpMethod = "get",
        value = "Retrieve changes",
        notes = "Retrieve all media changes since a certain update sequence.\n" +
            "By submitting an optional profile argument only changes for this argument are emitted.",
        position = 11
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
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
                writer.write(Jackson2Mapper.INSTANCE.writeValueAsString(change));
            }
            writer.write("\n]}");
            writer.close();
        }
        return null;
    }

    private MediaObject handleNotFound(String id) {
        MediaObject answer = mediaService.load(id);
        if(answer == null) {
            throw new Exceptions().notFound("No media for id {}", id);
        }

        return answer;
    }

/*
    private Response filteredMediaResult(MediaResult result, String properties) {
        if(properties == null) {
            return Response.ok(result).build();
        }

        return Response.ok(MediaTransferResult.create(result, new PropertySelection(properties))).build();
    }

    private Response filteredAsMediaResult(MediaSearchResult searchResult, String properties) {
        if(properties == null) {
            return Response.ok(searchResult.asResult()).build();
        }

        return Response.ok(MediaTransferResult.create(searchResult.asResult(), new PropertySelection(properties))).build();
    }

    private Response filteredProgramResult(ProgramResult result, String properties) {
        if(properties == null) {
            return Response.ok(result).build();
        }

        return Response.ok(ProgramTransferResult.create(result, new PropertySelection(properties))).build();
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
*/
}
