/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;

import nl.vpro.api.Settings;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.RepositoryType;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.domain.media.Schedule;
import nl.vpro.util.FilteringIterator;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@ManagedResource(objectName = "nl.vpro.api:name=MediaService")
@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final ProfileService profileService;

    private final MediaRepository mediaLoadRepository;

    private final MediaSearchRepository mediaSearchRepository;

    private final QuerySearchRepository querySearchRepository;

    private final Settings settings;

    @Autowired
    public MediaServiceImpl(
        ProfileService profileService,
        @Named("mediaLoadRepository") MediaRepository mediaRepository,
        MediaSearchRepository mediaSearchRepository,
        @Named("mediaQueryRepository") QuerySearchRepository querySearchRepository,
        Settings settings

    ) {
        this.profileService = profileService;
        this.mediaLoadRepository = mediaRepository;
        this.mediaSearchRepository = mediaSearchRepository;
        this.querySearchRepository = querySearchRepository;
        this.settings = settings;
    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }

    private static long DIVIDING_SINCE = LocalDate.of(2000, 1,1).atStartOfDay(Schedule.ZONE_ID).toInstant().toEpochMilli();

    @Override
    public Iterator<Change> changes(final String profile, final Long since, Instant publishedSince, final Order order, final Integer max, final Long keepAlive) throws ProfileNotFoundException {
        if (since != null && publishedSince != null){
            throw new IllegalArgumentException("Cannot use both since and publishSince arguments!");
        }
        if (since != null) {
            if (since > DIVIDING_SINCE) { // Certainly using
                return changesWitchES(profile, Instant.ofEpochMilli(since), order, max, keepAlive);
            } else {
                Iterator<Change> iterator = changesWithCouchDB(profile, since, order, max, keepAlive);
                if (settings.changesRepository == RepositoryType.ELASTICSEARCH) {
                    iterator = Iterators.transform(iterator,
                        c -> {
                            if (c != null && c.isTail()) {
                                c.setSequence(Instant.now().toEpochMilli());
                                LOG.info("Forcing changes repository to ES by upping sequence {}", c);
                            }
                            return c;
                        }
                    );

                }
                return iterator;
            }
        } else {
            // caller is aware of 'publishedSince' argument, so she doesn't need the 'sequences' any more.
            return
                Iterators.transform(changesWitchES(profile, publishedSince, order, max, keepAlive), c -> {
                    if (c != null) {
                        c.setSequence(null);
                    }
                    return c;});
        }
    }

    @Deprecated
    protected Iterator<Change> changesWithCouchDB(final String profile, final Long since, final Order order, final Integer max, final Long keepAlive) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null ? null : profileService.getMediaProfileDefinition(profile, since); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaLoadRepository.changes(since, currentProfile, previousProfile, order, max, keepAlive);
    }


    protected Iterator<Change> changesWitchES(final String profile, final Instant since, final Order order, final Integer max, final Long keepAlive) throws ProfileNotFoundException {
        ProfileDefinition<MediaObject> currentProfile = profileService.getMediaProfileDefinition(profile); //getCombinedProfile(profile, since);

        ProfileDefinition<MediaObject> previousProfile = since == null ? null : profileService.getMediaProfileDefinition(profile, since); //getCombinedProfile(profile, since);
        if (currentProfile == null && previousProfile == null && profile != null) {
            throw new ProfileNotFoundException("No such media profile " + profile);
        }
        return mediaSearchRepository.changes(since, currentProfile, previousProfile, order, max, keepAlive);
    }


    @Override
    public <T extends MediaObject> T findByMid(String mid) {
        return switchRepository(settings.loadRepository).findByMid(mid);
    }


    @Override
    public List<MediaObject> loadAll(List<String> ids) {
        return switchRepository(settings.loadRepository).loadAll(ids);
    }

    @Override
    public RedirectList redirects() {
        return switchRepository(settings.redirectsRepository).redirects();
    }

    @Override
    public MediaResult list(Order order, Long offset, Integer max) {
        return switchRepository(settings.listRepository).list(order, offset, max);
    }

    @Override
    public Iterator<MediaObject> iterate(String profile, MediaForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return switchRepository(settings.listRepository).iterate(getProfile(profile), form, offset, max, keepAlive);
    }

    @Override
    public MediaSearchResult find(String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    public MediaResult listMembers(MediaObject media, Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listMembers(media, order, offset, max);
    }

    @Override
    public MediaSearchResult findMembers(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findMembers(media, getProfile(profile), form, offset, max);
    }

    @Override
    public ProgramResult listEpisodes(MediaObject media, Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listEpisodes(media, order, offset, max);
    }

    @Override
    public ProgramSearchResult findEpisodes(MediaObject media, String profile, MediaForm form, Long offset, Integer max) {
        return mediaSearchRepository.findEpisodes(media, getProfile(profile), form, offset, max);
    }

    @Override
    public MediaResult listDescendants(MediaObject media, Order order, Long offset, Integer max) {
        return switchRepository(settings.membersRepository).listDescendants(media, order, offset, max);
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
