/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.constraint.media.Filter;
import nl.vpro.domain.constraint.media.MediaConstraints;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.util.FilteringIterator;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class MediaServiceImplTest {

    private final MediaRepository mediaRepository = Mockito.mock(MediaRepository.class);

    private final MediaSearchRepository mediaSearchRepository = Mockito.mock(MediaSearchRepository.class);

    private final ProfileService profileService = Mockito.mock(ProfileService.class);

    private final QuerySearchRepository querySearchRepository = Mockito.mock(QuerySearchRepository.class);


    private final MediaServiceImpl target = new MediaServiceImpl(profileService, mediaRepository, mediaSearchRepository, querySearchRepository);


    private ProfileDefinition<MediaObject> profileDefinition = new ProfileDefinition<>();

    private final MediaObject media = MediaTestDataBuilder.program().mid("POMS_12345").build();

    private final MediaForm form = new MediaForm();

    private final Long offset = 45l;

    private final Integer max = 25;

    @Before
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
        verify(mediaRepository).findByMid(media.getMid());
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

    @Test(expected = ProfileNotFoundException.class)
    public void testChangesProfileNotFound() {
        target.changes("notfound", 0l, null, 10, 100l);
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testIterateProfileNotFound() {
        target.iterate("notfound", null, 0l, 10, FilteringIterator.keepAlive((c) -> {}));
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testFindProfileNotFound() {
        target.find("notfound", null, 0l, 10);
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testFindMembersProfileNotFound() {
        target.findMembers(media, "notfound", null, 0l, 10);
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testFindEpisodesProfileNotFound() {
        target.findEpisodes(media, "notfound", null, 0l, 10);
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testFindDescendantsProfileNotFound() {
        target.findDescendants(media, "notfound", null, 0l, 10);
    }

    @Test(expected = ProfileNotFoundException.class)
    public void testFindRelatedProfileNotFound() {
        target.findRelated(media, "notfound", null, 10);
    }
}
