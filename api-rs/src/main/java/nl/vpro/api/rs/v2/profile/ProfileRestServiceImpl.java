/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.vpro.api.profile.ProfileService;
import nl.vpro.domain.api.profile.Profile;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class ProfileRestServiceImpl implements ProfileRestService {

    final private ProfileService profileService;

    @Autowired
    public ProfileRestServiceImpl(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public Profile load(String name, boolean mock) {
        if(mock == true) {
            return profileService.getProfiles().first();
        }

        return profileService.getProfile(name);
    }

}
