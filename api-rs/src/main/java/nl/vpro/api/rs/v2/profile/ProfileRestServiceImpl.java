/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wordnik.swagger.annotations.*;

import nl.vpro.api.rs.v2.exception.Exceptions;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.swagger.SwaggerApplication;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
@Path(ProfileRestService.PATH)
@Produces({MediaType.APPLICATION_XML + "; charset=utf-8", MediaType.APPLICATION_JSON})
@Api(value = ProfileRestService.PATH, position = 3)
public class ProfileRestServiceImpl implements ProfileRestService {

    private final ProfileService profileService;

    @Value("${api.profiles.expose}")
    private boolean expose;

    @Autowired
    public ProfileRestServiceImpl(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostConstruct
    private void init() {
        if(expose) {
            SwaggerApplication.inject(this);
        }
    }

    @GET
    @Path("/{name}")
    @ApiOperation(httpMethod = "get",
        value = "Load a profile",
        notes = "Retrieve a profile by its name and an optional point in time")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Client error"),
        @ApiResponse(code = 500, message = "Server error")
    })
    @Override
    public Profile load(
        @ApiParam(required = true) @PathParam("name") String name,
        @ApiParam(required = false, value = "Optional timestamp in millis from 1970") @QueryParam("time") Long time
    ) {
        Profile answer;

        if(time != null) {
            Date timestamp = new Date(time);
            answer = profileService.getProfile(name, timestamp);
        } else {
            answer = profileService.getProfile(name);
        }

        if(answer != null) {
            return answer;
        }

        throw Exceptions.notFound("Profile not found");
    }

}
