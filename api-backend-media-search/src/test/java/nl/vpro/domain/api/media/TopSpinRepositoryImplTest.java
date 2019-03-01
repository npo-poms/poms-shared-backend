package nl.vpro.domain.api.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import nl.vpro.domain.api.topspin.Recommendations;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.*;

public class TopSpinRepositoryImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(9999));

    @Test
    public void testNoExists() {
        wireMockRule.stubFor(get(urlEqualTo("/notexist")).willReturn(notFound()));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/{mediaId}";
        Recommendations forMid = repo.getForMid("notexist", null, null);
        assertNotNull(forMid);
        assertTrue(forMid.getRecommendations().isEmpty());
    }

    @Test
    public void testExists() throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream("/topspin-response.json"), StandardCharsets.UTF_8);
        wireMockRule.stubFor(get(urlEqualTo("/exists")).willReturn(okJson(json)));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/{mediaId}";
        Recommendations forMid = repo.getForMid("exists", null, null);
        assertEquals(87, forMid.getRecommendations().size());
    }

}
