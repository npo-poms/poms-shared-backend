/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nl.vpro.domain.api.profile.*;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.constraint.media.Filter;
import nl.vpro.domain.constraint.media.MediaConstraints;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.util.FilteringIterator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class MediaServiceImplTest {


    private final MediaSearchRepository mediaSearchRepository = Mockito.mock(MediaSearchRepository.class);

    private final ProfileService profileService = Mockito.mock(ProfileService.class);

    private final QuerySearchRepository querySearchRepository = Mockito.mock(QuerySearchRepository.class);

    private final MediaServiceImpl target = new MediaServiceImpl(profileService,  mediaSearchRepository, querySearchRepository, new SinceToTimeStampServiceImpl(), null);

    private ProfileDefinition<MediaObject> profileDefinition = new ProfileDefinition<>();

    private final MediaObject media = MediaTestDataBuilder.program().mid("POMS_12345").build();

    private final MediaForm form = new MediaForm();

    private final Long offset = 45L;

    private final Integer max = 25;

    @BeforeEach
    public void setUp() {
        Mockito.reset(profileService, mediaSearchRepository);
        Filter filter = new Filter();
        filter.setConstraint(MediaConstraints.and(
            MediaConstraints.broadcaster("VPRO"),
            MediaConstraints.hasImage()
        ));
        profileDefinition = new ProfileDefinition<>(filter);
        Profile profile = new Profile("vpro", null, profileDefinition);
        when(profileService.getProfile(eq("vpro"))).thenReturn(profile);
    }

    @Test
    public void testLoad()  {
        target.findByMid(media.getMid());
        verify(mediaSearchRepository).findByMid(media.getMid());
    }

    @Test
    public void testFind() {
        target.find("vpro", form, offset, max);
        verify(mediaSearchRepository).find(profileDefinition, form, offset, max);
    }

    @Test
    public void testFindMembers()  {
        target.findMembers(media, "vpro", form, offset, max);
        verify(mediaSearchRepository).findMembers(media, profileDefinition, form, offset, max);
    }

    @Test
    public void testFindEpisodes() {
        target.findEpisodes(media, "vpro", form, offset, max);
        verify(mediaSearchRepository).findEpisodes(media, profileDefinition, form, offset, max);
    }

    @Test
    public void testFindDescendants() {
        target.findDescendants(media, "vpro", form, offset, max);
        verify(mediaSearchRepository).findDescendants(media, profileDefinition, form, offset, max);
    }

    @Test
    public void testFindRelated()  {
        target.findRelated(media, "vpro", form, max);
        verify(mediaSearchRepository).findRelated(media, profileDefinition, form, max);
    }

    @Test
    public void testChangesProfileNotFound() {
        assertThatThrownBy(() ->
            target.changes("notfound", true, Instant.EPOCH,  null, null, 10, 100L, false, null)
        ).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    public void testIterateProfileNotFound() {
        assertThatThrownBy(() ->
            target.iterate("notfound", null, 0L, 10, FilteringIterator.keepAliveWithoutBreaks((c) -> {}))
        ).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    public void testFindProfileNotFound() {
        assertThatThrownBy(() ->
            target.find("notfound", null, 0L, 10)
        ).isInstanceOf(ProfileNotFoundException.class);

    }

    @Test
    public void testFindMembersProfileNotFound() {
        assertThatThrownBy(() ->
                target.findMembers(media, "notfound", null, 0L, 10)
        ).isInstanceOf(ProfileNotFoundException.class);

    }

    @Test
    public void testFindEpisodesProfileNotFound() {
        assertThatThrownBy(() ->
            target.findEpisodes(media, "notfound", null, 0L, 10)
        ).isInstanceOf(ProfileNotFoundException.class);

    }

    @Test
    public void testFindDescendantsProfileNotFound() {
        assertThatThrownBy(() ->
            target.findDescendants(media, "notfound", null, 0L, 10)
        ).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    public void testFindRelatedProfileNotFound() {
        assertThatThrownBy(() ->
            target.findRelated(media, "notfound", null, 10)
        ).isInstanceOf(ProfileNotFoundException.class);

    }
}
