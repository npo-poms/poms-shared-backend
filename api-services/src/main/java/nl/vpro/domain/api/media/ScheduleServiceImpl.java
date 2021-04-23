/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.Instant;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import nl.vpro.domain.Roles;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Net;

/**
 * @author rico
 */
@Service
@PreAuthorize("hasRole('ROLE_EVERYBODY_EXCLUDED')")
public class ScheduleServiceImpl implements ScheduleService {

    private final ProfileService profileRepository;

    private final ScheduleRepository searchRepository;


    @Autowired
    public ScheduleServiceImpl(
        ProfileService profileService,
        @Named("scheduleSearchRepository") ScheduleRepository searchRepository
        ) {
        this.profileRepository = profileService;
        this.searchRepository = searchRepository;
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.list")
    public ScheduleResult list(Instant start, Instant stop, Order order, long offset, Integer max) {
        return searchRepository.listSchedules(start, stop, order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.listChannel")
    public ScheduleResult list(Channel channel, Instant start, Instant stop, Order order, long offset, Integer max) {
        return searchRepository.listSchedules(channel, start, stop, order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.listNet")
    public ScheduleResult list(Net net, Instant start, Instant stop, Order order, long offset, Integer max) {
        return searchRepository.listSchedules(net, start, stop, order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.listForBroadcaster")
    public ScheduleResult listForBroadcaster(String broadcaster, Instant start, Instant stop, Order order, long offset, Integer max) {
        return searchRepository.listSchedulesForBroadcaster(broadcaster, start, stop, order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.listForAncestor")
    public ScheduleResult listForAncestor(String mediaId, Instant start, Instant stop, Order order, long offset, Integer max) {
        return searchRepository.listSchedulesForAncestor(mediaId, start, stop, order, offset, max);
    }


    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("ScheduleService.find")
    public ScheduleSearchResult find(ScheduleForm form, Order sort, String profile, long offset, Integer max) {
        ProfileDefinition<MediaObject> profileDefinition = profileRepository.getMediaProfileDefinition(profile);
        return searchRepository.findSchedules(profileDefinition, form, sort, offset, max);
    }

}
