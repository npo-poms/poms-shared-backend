package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.domain.api.topspin.Recommendations;

import static org.junit.Assert.assertNotNull;

@Slf4j
public class TopSpinRepositoryImplITest {

    @Test
    public void testNew() {
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "https://hub.npo-data.nl/api/v4/media/{mediaId}/recommendations/{class}";
        Recommendations forMid = repo.getForMid("RBX_BV_13063322", null, "related-broadcasts");
        assertNotNull(forMid);
        log.info("{}", forMid.getRecommendations());
    }


    @Test
    @Ignore("I suppose the endpoint is dropped now")
    public void testOld() {
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "https://api.npo.nl/api/v3/recommendations/related/{mediaId}";
        Recommendations forMid = repo.getForMid("RBX_BV_13063322", null, null);
        assertNotNull(forMid);
        log.info("{}", forMid.getRecommendations());
    }
}
