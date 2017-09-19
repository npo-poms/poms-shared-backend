/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;

import nl.vpro.api.Settings;
import nl.vpro.domain.Roles;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.api.topspin.Recommendation;
import nl.vpro.domain.api.topspin.Recommendations;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.util.FilteringIterator;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@ManagedResource(objectName = "nl.vpro.api:name=MediaService")
@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final ProfileService profileService;

    private final MediaRepository mediaLoadRepository;

    private final MediaSearchRepository mediaSearchRepository;

    private final QuerySearchRepository querySearchRepository;

    private final TopSpinRepository topSpinRepository;

    private final Settings settings;

    private final SinceToTimeStampService sinceToTimeStampService;

    @Autowired
    public MediaServiceImpl(
        ProfileService profileService,
        @Named("mediaLoadRepository") MediaRepository mediaRepository,
        MediaSearchRepository mediaSearchRepository,
        @Named("mediaQueryRepository") QuerySearchRepository querySearchRepository,
        SinceToTimeStampService sinceToTimeStampService,
        TopSpinRepository topSpinRepository,
        Settings settings

    ) {
        this.profileService = profileService;
        this.mediaLoadRepository = mediaRepository;
        this.mediaSearchRepository = mediaSearchRepository;
        this.querySearchRepository = querySearchRepository;
        this.topSpinRepository = topSpinRepository;
        this.sinceToTimeStampService = sinceToTimeStampService;
        this.settings = settings;
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public SuggestResult suggest(String input, String profile, Integer max) {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }


    @Override
    @PreAuthorize(Roles.API_CHANGES_USER)
    public Iterator<MediaChange> changes(final String profile,  final boolean profileCheck, final Instant since, String mid, final Order order, final Integer max, final Long keepAlive, boolean withSequences, Deletes deletes) throws ProfileNotFoundException {
        if (withSequences) {
            if (since.isAfter(SinceToTimeStampService.DIVIDING_SINCE)) { // Certainly using ES
                return changesWithES(profile, profileCheck, since, mid,  order, max, keepAlive, deletes);
            } else {
                Iterator<MediaChange> iterator;
                if (settings.changesRepository == RepositoryType.ELASTICSEARCH) {
                    Instant i = sinceToTimeStampService.getInstance(since.toEpochMilli());
                    log.info("Since {} is couchdb like, taking {}. Explicitely configured to use elastic search for changes feed. Note that client don't receive sequences.", since.toEpochMilli(), i);


                    iterator = changesWithES(profile, profileCheck, i, mid, order, max, keepAlive, deletes);
                } else {
                    //noinspection deprecation
                    iterator = changesWithCouchDB(profile, profileCheck, since, order, max, keepAlive, deletes);
                }
                return iterator;
            }
        } else {
            // caller is aware of 'publishedSince' argument, so she doesn't need the 'sequences' any more.
            return
                Iterators.transform(changesWithES(profile, profileCheck, since, mid, order, max, keepAlive, deletes), c -> {
                    if (c != null) {
                        c.setSequence(null);
                    }
                    return c;});
        }
    }

    @Deprecated
    protected Iterator<MediaChange> changesWithCouchDB(final String profile, final boolean profileCheck, Instant since, final Order order, final Integer max, final Long keepAlive, Deletes deletes) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null || ! profileCheck ? null : profileService.getMediaProfileDefinition(profile, sinceToTimeStampService.getInstance(since.toEpochMilli())); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaLoadRepository.changes(since.toEpochMilli(),  currentProfile, previousProfile, order, max, keepAlive);
    }


    protected Iterator<MediaChange> changesWithES(final String profile, boolean profileCheck, final Instant since, String mid, final Order order, final Integer max, final Long keepAlive, Deletes deletes) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null || ! profileCheck ? currentProfile : profileService.getMediaProfileDefinition(profile, since); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaSearchRepository.changes(since, mid, currentProfile, previousProfile, order, max, keepAlive, deletes);
    }


    @Override
    @PreAuthorize(Roles.API_USER)
    public <T extends MediaObject> T findByMid(String mid) {
        return switchRepository(settings.loadRepository).findByMid(mid);
    }


    @Override
    @PreAuthorize(Roles.API_USER)
    public List<MediaObject> loadAll(List<String> ids) {
        return switchRepository(settings.loadRepository).loadAll(ids);
    }

    @Override
    //@PreAuthorize("permitAll()")
    public RedirectList redirects() {
        return switchRepository(settings.redirectsRepository).redirects();
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaResult list(Order order, Long offset, Integer max) {
        return switchRepository(settings.listRepository).list(order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public Iterator<MediaObject> iterate(String profile, MediaForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return switchRepository(settings.listRepository).iterate(getProfile(profile), form, offset, max, keepAlive);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaSearchResult find(String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaResult listMembers(MediaObject media, String profile, Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listMembers(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaSearchResult findMembers(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findMembers(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public ProgramResult listEpisodes(MediaObject media, String profile,  Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listEpisodes(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public ProgramSearchResult findEpisodes(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findEpisodes(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaResult listDescendants(MediaObject media, String profile, Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listDescendants(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaSearchResult findDescendants(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findDescendants(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaSearchResult findRelated(MediaObject media, String profile, MediaForm form, Integer max) {
        return mediaSearchRepository.findRelated(media, getProfile(profile), form, max);
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaSearchResult findRelatedInTopspin(MediaObject media, String profile, MediaForm form, Integer max) {
        Recommendations recommendations = topSpinRepository.getForMid(media.getMid());
        List<MediaObject> mediaObjects = loadAll(recommendations.getRecommendations().stream().map(Recommendation::getMidRef).collect(Collectors.toList()));
        Predicate<MediaObject> filter = (mo) -> true;
        if (profile != null) {
            filter = getProfile(profile);
        }
        if (form != null) {
            filter = filter.and(form);
        }
        List<SearchResultItem<? extends MediaObject>> filtered = mediaObjects.stream().filter(filter).limit(max).map(SearchResultItem::new).collect(Collectors.toList());
        return new MediaSearchResult(filtered, 0L, max, filtered.size());
    }

    @Override
    @PreAuthorize(Roles.API_USER)
    public MediaType getType(final String id) {
        MediaObject owner = findByMid(id);
        return owner != null ? MediaType.getMediaType(owner) : null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Optional<String> redirect(String mid) {
        return switchRepository(settings.redirectsRepository).redirect(mid);

    }

    private MediaRepository switchRepository(RepositoryType repository) {
        return RepositoryType.switchRepository(repository, mediaLoadRepository, mediaSearchRepository);
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
