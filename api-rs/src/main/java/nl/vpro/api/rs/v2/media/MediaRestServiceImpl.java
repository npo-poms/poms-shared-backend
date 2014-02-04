/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.media;

import com.wordnik.swagger.annotations.*;
import nl.vpro.api.rs.v2.exception.Exceptions;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.api.transfer.ResultFilter;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.bind.Jackson2Mapper;
import nl.vpro.swagger.SwaggerApplication;
import nl.vpro.transfer.media.MediaObjectPropertySelectionFunction;
import nl.vpro.transfer.media.PropertySelection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import static nl.vpro.api.rs.v2.Util.exception;
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
            MediaResult searchResult = mediaService.list(offset, max);
            return properties==null ? searchResult : ResultFilter.filter(searchResult, properties);
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
            MediaSearchResult result = mediaService.find(profile, form, offset, max);
            return properties == null ? result : ResultFilter.filter(result, properties);
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
        try {
            MediaObject mediaObject = handleNotFound(id);
            if(properties == null) {
                return mediaObject;
            }
            mediaObject = new MediaObjectPropertySelectionFunction(new PropertySelection(properties)).apply(mediaObject);
            return mediaObject;
        } catch(Exception e) {
            throw exception(e);
        }
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
        try {

            MediaResult result = mediaService.listMembers(id, offset, max);
            if(result == null) {
                throw Exceptions.notFound("No members found for id %s ", id);
            }
            if(result.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? result : ResultFilter.filter(result, properties);
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
            MediaSearchResult members = mediaService.findMembers(id, profile, form, offset, max);

            if(members == null) {
                throw Exceptions.notFound("No members found for id %s ", id);
            }
            if(members.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? members : ResultFilter.filter(members, properties);
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
            ProgramResult episodes = mediaService.listEpisodes(id, offset, max);
            if(episodes.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? episodes : ResultFilter.filter(episodes, properties);
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
            ProgramSearchResult episodes = mediaService.findEpisodes(id, profile, form, offset, max);
            if(episodes.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? episodes : ResultFilter.filter(episodes, properties);
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

            MediaResult descendants = mediaService.listDescendants(id, offset, max);
            if(descendants.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? descendants : ResultFilter.filter(descendants, properties);
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
            MediaSearchResult descendants = mediaService.findDescendants(id, profile, form, offset, max);
            if(descendants.getSize() == 0) {
                handleNotFound(id);
            }
            return properties == null ? descendants : ResultFilter.filter(descendants, properties);

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
        @ApiParam(required = true) @PathParam("id") String id,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        try {
            MediaResult related = mediaService.listRelated(id, offset, max);
            return properties == null ? related : ResultFilter.filter(related, properties);
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
            MediaSearchResult related = mediaService.findRelated(id, profile, form, offset, max);
            return properties == null ? related : ResultFilter.filter(related, properties);
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
    public Change changes(
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
    
}
