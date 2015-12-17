/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.util.FilteringIterator;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
public class MediaServiceImpl implements MediaService {

    private final ProfileService profileService;

    private final MediaRepository mediaRepository;

    private final MediaSearchRepository mediaSearchRepository;

    private final QuerySearchRepository querySearchRepository;

    @Autowired
    public MediaServiceImpl(
        ProfileService profileService,
        MediaRepository mediaRepository,
        MediaSearchRepository mediaSearchRepository,
        @Named("mediaQueryRepository") QuerySearchRepository querySearchRepository) {
        this.profileService = profileService;
        this.mediaRepository = mediaRepository;
        this.mediaSearchRepository = mediaSearchRepository;
        this.querySearchRepository = querySearchRepository;
    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }

    @Override
    public Iterator<Change> changes(final String profile, final Long since, final Order order, final Integer max, final Long keepAlive) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null ? null : profileService.getMediaProfileDefinition(profile, since); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaRepository.changes(since, currentProfile, previousProfile, order, max, keepAlive);
    }

    @Override
    public <T extends MediaObject> T findByMid(String mid) {
        return mediaRepository.findByMid(mid);
    }

    @Override
    public List<MediaObject> loadAll(List<String> ids) {
        return mediaRepository.loadAll(ids);
    }

    @Override
    public RedirectList redirects() {
        return mediaRepository.redirects();
    }

    @Override
    public MediaResult list(Order order, Long offset, Integer max) {
        return mediaRepository.list(order, offset, max);
    }

    @Override
    public Iterator<MediaObject> iterate(String profile, MediaForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return mediaRepository.iterate(getProfile(profile), form, max, offset, keepAlive);
    }

    @Override
    public MediaSearchResult find(String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    public MediaResult listMembers(MediaObject media, Order order, Long offset, Integer max) {
        return mediaRepository.listMembers(media, order, offset, max);
    }

    @Override
    public MediaSearchResult findMembers(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findMembers(media, getProfile(profile), form, offset, max);
    }

    @Override
    public ProgramResult listEpisodes(MediaObject media, Order order, Long offset, Integer max) {
        return mediaRepository.listEpisodes(media, order, offset, max);
    }

    @Override
    public ProgramSearchResult findEpisodes(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findEpisodes(media, getProfile(profile), form, offset, max);
    }

    @Override
    public MediaResult listDescendants(MediaObject media, Order order, Long offset, Integer max) {
        return mediaRepository.listDescendants(media, order, offset, max);
    }

    @Override
    public MediaSearchResult findDescendants(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findDescendants(media, getProfile(profile), form, offset, max);
    }

    @Override
    public MediaSearchResult findRelated(MediaObject media, String profile, MediaForm form, Integer max) {
        return mediaSearchRepository.findRelated(media, getProfile(profile), form, max);
    }

    @Override
    public MediaType getType(final String id) {
        MediaObject owner = findByMid(id);
        return owner != null ? MediaType.getMediaType(owner) : null;
    }

    @Override
    public Optional<String> redirect(String mid) {
        return mediaRepository.redirect(mid);

    }

    private ProfileDefinition<MediaObject> getProfile(String profile) {
        if (profile == null || "".equals(profile) // handy for scripting (profile=$2 and so on...)
            ) {
            return null;
        }
        Profile p = profileService.getProfile(profile);
        if (p == null) {
            throw new ProfileNotFoundException(profile);
        }
        return p.getMediaProfile();

    }

}
