package nl.vpro.domain.api.media;


import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

import nl.vpro.domain.api.profile.*;
import nl.vpro.domain.api.topspin.Recommendation;
import nl.vpro.domain.api.topspin.Recommendations;
import nl.vpro.domain.constraint.media.*;
import nl.vpro.domain.media.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class TopspinTest {

    MediaSearchRepository mediaRepository = Mockito.mock(MediaSearchRepository.class);

    ProfileService profileService = Mockito.mock(ProfileService.class);

    TopSpinRepository topSpinRepository = Mockito.mock(TopSpinRepository.class);

    private final MediaServiceImpl target = new MediaServiceImpl(profileService, mediaRepository, null, new SinceToTimeStampServiceImpl(), topSpinRepository);

    Program program;

    @BeforeEach
    public void setup() {

        // Given
        String startingMid = "mid0";
        String recommendedMid1 = "mid1";
        String recommendedMid2 = "mid2";
        String recommendedMid3 = "mid3";
        Recommendations recommendations = new Recommendations();
        recommendations.setRecommendations(ImmutableList.of(
            new Recommendation(recommendedMid1),
            new Recommendation(recommendedMid2),
            new Recommendation(recommendedMid3)
        ));
        program = MediaBuilder.program(ProgramType.BROADCAST)
            .mid(startingMid)
            .mainTitle("You're interested in me")
            .build();

        Program recommendedProgram1 = MediaBuilder.program(ProgramType.BROADCAST)
            .mid(recommendedMid1)
            .mainTitle("This might interest you")
            .avType(AVType.VIDEO)
            .build();
        Program recommendedProgram2 = MediaBuilder.program(ProgramType.BROADCAST)
            .mid(recommendedMid2)
            .broadcasters("EO")
            .mainTitle("I don't fit your profile")
            .avType(AVType.VIDEO)
            .build();
        Program recommendedProgram3 = MediaBuilder.program(ProgramType.BROADCAST)
            .mid(recommendedMid3)
            .mainTitle("I don't fit your media form")
            .avType(AVType.AUDIO)
            .build();
        List<MediaObject> objects = ImmutableList.of(recommendedProgram1, recommendedProgram2, recommendedProgram3);

        Filter filter = new Filter();
        filter.setConstraint(new Not(new BroadcasterConstraint("EO")));
        ProfileDefinition<MediaObject> notEOProfile = new ProfileDefinition<>(filter);
        Profile profile = new Profile("noteoprofile", null, notEOProfile);

        // When

        when(topSpinRepository.getForMid(startingMid, null, null)).thenReturn(recommendations);
        doReturn(objects).when(mediaRepository).loadAll(anyList());
        when(mediaRepository.loadAll(anyList())).thenReturn(objects);
        when(profileService.getProfile("noteoprofile")).thenReturn(profile);
    }

    @Test
    public void topspinNoFilter() {
        MediaSearchResult result = target.findRelatedInTopspin(program, null, null, 3, null, null);
        assertThat(result).hasSize(3);
    }

    @Test
    public void topspinProfileFilter() {
        MediaSearchResult result = target.findRelatedInTopspin(program, "noteoprofile", null, 3, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    public void topspinFormFilter() {
        MediaForm form = MediaFormBuilder.form().avTypes(AVType.VIDEO).build();
        MediaSearchResult result = target.findRelatedInTopspin(program, null, form, 3, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    public void topspinProfileAndFormFilter() {
        MediaForm form = MediaFormBuilder.form().avTypes(AVType.VIDEO).build();
        MediaSearchResult result = target.findRelatedInTopspin(program, "noteoprofile", form, 3, null, null);
        assertThat(result).hasSize(1);
    }
}
