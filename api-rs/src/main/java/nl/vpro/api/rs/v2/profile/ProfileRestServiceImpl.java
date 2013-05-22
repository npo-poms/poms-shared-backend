/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.profile;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.vpro.api.profile.ProfileService;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.profile.Profile;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class ProfileRestServiceImpl implements ProfileRestService {

    private ProfileService profileService;

    @Autowired
    public ProfileRestServiceImpl(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public Result<Profile> list(Integer offset, Integer max) {
        List<Profile> profiles = new ArrayList<>(profileService.getProfiles());
        return new Result<>(profiles.subList(offset, offset + max), offset, profiles.size());
    }

    @Override
    public Profile load(String name) {
        return profileService.getProfile(name);
    }

}
