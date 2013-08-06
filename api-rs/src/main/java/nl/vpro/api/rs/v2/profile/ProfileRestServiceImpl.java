/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.swagger.SwaggerApplication;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class ProfileRestServiceImpl implements ProfileRestService {

    final private ProfileService profileService;

    @Value("${api.profile.expose}")
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

    @Override
    public Profile load(String name, boolean mock) {
        if(mock) {
            return profileService.getProfiles().first();
        }

        return profileService.getProfile(name);
    }

}
