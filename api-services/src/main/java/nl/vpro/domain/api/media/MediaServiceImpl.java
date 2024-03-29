/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.Predicates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.*;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.api.topspin.Recommendation;
import nl.vpro.domain.api.topspin.Recommendations;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.FilteringIterator;

import static nl.vpro.domain.Roles.HAS_API_CHANGES_ROLE;
import static nl.vpro.domain.Roles.HAS_API_ROLE;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Service
@Log4j2
public class MediaServiceImpl implements MediaService {

    private final ProfileService profileService;

    private final MediaSearchRepository mediaSearchRepository;

    private final QuerySearchRepository querySearchRepository;

    private final TopSpinRepository topSpinRepository;

    private final SinceToTimeStampService sinceToTimeStampService;

    @Autowired
    public MediaServiceImpl(
        ProfileService profileService,
        MediaSearchRepository mediaSearchRepository,
        @Named("mediaQueryRepository") QuerySearchRepository querySearchRepository,
        SinceToTimeStampService sinceToTimeStampService,
        TopSpinRepository topSpinRepository

    ) {
        this.profileService = profileService;
        this.mediaSearchRepository = mediaSearchRepository;
        this.querySearchRepository = querySearchRepository;
        this.topSpinRepository = topSpinRepository;
        this.sinceToTimeStampService = sinceToTimeStampService;
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.suggest")
    public SuggestResult suggest(String input, String profile, Integer max) throws ProfileNotFoundException {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }

    @Override
    @PreAuthorize(HAS_API_CHANGES_ROLE)
    public CloseableIterator<MediaChange> changes(
        final String profile,
        final Boolean profileCheck,
        final Instant since,
        final String mid,
        @Nullable final Order order,
        final Integer max,
        final boolean withSequences,
        final Deletes deletes,
        final Tail tail,
        final Predicate<MediaChange> postFilter) throws ProfileNotFoundException {
        if (withSequences) {
            if (since.isAfter(SinceToTimeStampService.DIVIDING_SINCE)) { // Certainly using ES
                return changesWithES(profile, since, mid,  order, max, deletes, tail, postFilter);
            } else {
                final Instant i = sinceToTimeStampService.getInstance(since.toEpochMilli());
                log.info("Since {} is couchdb like, taking {}. Explicitly configured to use elastic search for changes feed. Note that clients don't receive sequences.", since.toEpochMilli(), i);
                return changesWithES(profile, i, mid, order, max, deletes, tail, postFilter);
            }
        } else {
            // caller is aware of 'publishedSince' argument, so she doesn't need the 'sequences' anymore.
            return
                FilteringIterator.<MediaChange>builder()
                    .wrapped(changesWithES(profile, since, mid, order, max, deletes, tail, postFilter))
                    .filter((c) -> {
                        if (c != null) {
                            c.setSequence(null);
                        }
                        return true;
                    })
                    .build();
        }
    }


    protected CloseableIterator<MediaChange> changesWithES(
        final String profile,
        final Instant since,
        final String mid,
        final @Nullable Order order,
        final Integer max,
        final @Nullable Deletes deletes,
        final Tail tail,
        final @Nullable Predicate<MediaChange> postFilter) throws ProfileNotFoundException {
        final ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        if (currentProfile == null && profile != null) {
            throw new ProfileNotFoundException(profile);
        }
        return mediaSearchRepository.changes(
            since, mid, currentProfile, order, max, deletes, tail, postFilter);
    }


    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findByMid")
    public <T extends MediaObject> T findByMid(boolean loadDeleted, String mid) {
        return mediaSearchRepository.findByMid(loadDeleted, mid);
    }


    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.loadAll")
    public List<MediaObject> loadAll(List<String> ids) {
        return mediaSearchRepository.loadAll(false, ids);
    }

    @Override
    //@PreAuthorize("permitAll()")
    @Cacheable("MediaRestService.redirects")
    public RedirectList redirects() {
        return mediaSearchRepository.redirects();
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.list")
    public MediaResult list(Order order, Long offset, Integer max) {
        return mediaSearchRepository.list(order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public CloseableIterator<MediaObject> iterate(String profile, MediaForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return mediaSearchRepository.iterate(getProfile(profile), form, offset, max, keepAlive);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.find")
    public MediaSearchResult find(String profile, MediaForm form, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.listMembers")
    public MediaResult listMembers(MediaObject media, String profile, Order order, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.listMembers(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findMembers")
    public MediaSearchResult findMembers(MediaObject media, String profile, MediaForm form, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.findMembers(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.listEpisodes")
    public ProgramResult listEpisodes(MediaObject media, String profile,  Order order, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.listEpisodes(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findEpisodes")
    public ProgramSearchResult findEpisodes(MediaObject media, String profile, MediaForm form, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.findEpisodes(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.listDescendants")
    public MediaResult listDescendants(MediaObject media, String profile, Order order, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.listDescendants(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findDescendants")
    public MediaSearchResult findDescendants(MediaObject media, String profile, MediaForm form, Long offset, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.findDescendants(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findRelated")
    public MediaSearchResult findRelated(MediaObject media, String profile, MediaForm form, Integer max) throws ProfileNotFoundException {
        return mediaSearchRepository.findRelated(media, getProfile(profile), form, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.findRelatedInTopspin")
    public MediaSearchResult findRelatedInTopspin(MediaObject media, String profile, MediaForm form, Integer max, String partyId, String clazz) throws ProfileNotFoundException {
        Recommendations recommendations = topSpinRepository.getForMid(media.getMid(), partyId, clazz);

        List<MediaObject> mediaObjects = loadAll(
            recommendations.getRecommendations().stream()
                .map(Recommendation::getMidRef)
                .collect(Collectors.toList()));
        Predicate<MediaObject> filter = (mo) -> true;
        if (profile != null) {
            filter = getProfile(profile);
        }
        if (form != null) {
            filter = filter == null ? form : filter.and(form);
        }
        final List<SearchResultItem<? extends MediaObject>> filtered = mediaObjects.stream()
            .filter(filter == null ? Predicates.alwaysTrue() : filter)
            .limit(max)
            .map(SearchResultItem::new)
            .collect(Collectors.toList());
        for (SearchResultItem<? extends MediaObject> f : filtered) {
            for (Recommendation r : recommendations) {
                if (r.getMidRef().equals(f.getResult().getMid())) {
                    f.setScore(r.getScore());
                }
            }
        }
        return new MediaSearchResult(filtered, 0L, max, Result.Total.equalsTo(filtered.size()));
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    @Cacheable("MediaRestService.getType")
    public MediaType getType(final String id) {
        MediaObject owner = findByMid(id);
        return owner != null ? MediaType.getMediaType(owner) : null;
    }

    @Override
    @PreAuthorize("permitAll()")
    @Cacheable("MediaRestService.redirect")
    public Optional<String> redirect(String mid) {
        return mediaSearchRepository.redirect(mid);

    }

    @Nullable
    private ProfileDefinition<MediaObject> getProfile(String profile) throws ProfileNotFoundException {
        if (profile == null || profile.isEmpty() // handy for scripting (profile=$2 and so on...)
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
