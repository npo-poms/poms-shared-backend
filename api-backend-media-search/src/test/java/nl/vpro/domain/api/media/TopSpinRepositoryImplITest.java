package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.api.topspin.Recommendations;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TopSpinRepositoryImplITest {

    @Test
    public void testNew() {
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "https://hub.npo-data.nl/api/v4/media/{mediaId}/recommendations/{class}";
        Recommendations forMid = repo.getForMid("RBX_BV_13063322", null, "related-broadcasts");
        assertThat(forMid).isNotNull();
        log.info("{}", forMid.getRecommendations());
    }


    @Test
    @Disabled("I suppose the endpoint is dropped now")
    public void testOld() {
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "https://api.npo.nl/api/v3/recommendations/related/{mediaId}";
        Recommendations forMid = repo.getForMid("RBX_BV_13063322", null, null);
        assertThat(forMid).isNotNull();
        log.info("{}", forMid.getRecommendations());
    }
}
