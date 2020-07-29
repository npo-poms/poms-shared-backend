/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
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
@ManagedResource(objectName = "nl.vpro.api:name=MediaService")
@Service
@Slf4j
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
    public SuggestResult suggest(String input, String profile, Integer max) {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }


    @Override
    @PreAuthorize(HAS_API_CHANGES_ROLE)
    public CloseableIterator<MediaChange> changes(final String profile, final boolean profileCheck, final Instant since, String mid, final Order order, final Integer max, final Long keepAlive, boolean withSequences, Deletes deletes, Tail tail) throws ProfileNotFoundException {
        if (withSequences) {
            if (since.isAfter(SinceToTimeStampService.DIVIDING_SINCE)) { // Certainly using ES
                return changesWithES(profile, profileCheck, since, mid,  order, max, keepAlive, deletes, tail);
            } else {
                CloseableIterator<MediaChange> iterator;

                Instant i = sinceToTimeStampService.getInstance(since.toEpochMilli());
                log.info("Since {} is couchdb like, taking {}. Explicitely configured to use elastic search for changes feed. Note that clients don't receive sequences.", since.toEpochMilli(), i);
                iterator = changesWithES(profile, profileCheck, i, mid, order, max, keepAlive, deletes, tail);
                return iterator;
            }
        } else {
            // caller is aware of 'publishedSince' argument, so she doesn't need the 'sequences' any more.
            return
                FilteringIterator.<MediaChange>builder()
                    .wrapped(changesWithES(profile, profileCheck, since, mid, order, max, keepAlive, deletes, tail))
                    .filter((c) -> {
                        if (c != null) {
                            c.setSequence(null);
                        }
                        return true;
                    })
                    .build();
        }
    }


    protected CloseableIterator<MediaChange> changesWithES(final String profile, boolean profileCheck, final Instant since, String mid, final Order order, final Integer max, final Long keepAlive, Deletes deletes, Tail tail) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null || ! profileCheck ? currentProfile : profileService.getMediaProfileDefinition(profile, since); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaSearchRepository.changes(since, mid, currentProfile, previousProfile, order, max, keepAlive, deletes, tail);
    }


    @Override
    @PreAuthorize(HAS_API_ROLE)
    public <T extends MediaObject> T findByMid(boolean loadDeleted, String mid) {
        return mediaSearchRepository.findByMid(loadDeleted, mid);
    }


    @Override
    @PreAuthorize(HAS_API_ROLE)
    public List<MediaObject> loadAll(List<String> ids) {
        return mediaSearchRepository.loadAll(false, ids);
    }

    @Override
    //@PreAuthorize("permitAll()")
    public RedirectList redirects() {
        return mediaSearchRepository.redirects();
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
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
    public MediaSearchResult find(String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaResult listMembers(MediaObject media, String profile, Order order, Long offset, Integer max) {
        return mediaSearchRepository.listMembers(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaSearchResult findMembers(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findMembers(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public ProgramResult listEpisodes(MediaObject media, String profile,  Order order, Long offset, Integer max) {
        return mediaSearchRepository.listEpisodes(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public ProgramSearchResult findEpisodes(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findEpisodes(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaResult listDescendants(MediaObject media, String profile, Order order, Long offset, Integer max) {
        return mediaSearchRepository.listDescendants(media, getProfile(profile), order, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaSearchResult findDescendants(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findDescendants(media, getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaSearchResult findRelated(MediaObject media, String profile, MediaForm form, Integer max) {
        return mediaSearchRepository.findRelated(media, getProfile(profile), form, max);
    }

    @Override
    @PreAuthorize(HAS_API_ROLE)
    public MediaSearchResult findRelatedInTopspin(MediaObject media, String profile, MediaForm form, Integer max, String partyId, String clazz) {
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
        List<SearchResultItem<? extends MediaObject>> filtered = mediaObjects.stream()
            .filter(filter)
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
    public MediaType getType(final String id) {
        MediaObject owner = findByMid(id);
        return owner != null ? MediaType.getMediaType(owner) : null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Optional<String> redirect(String mid) {
        return mediaSearchRepository.redirect(mid);

    }

    @Nullable
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
