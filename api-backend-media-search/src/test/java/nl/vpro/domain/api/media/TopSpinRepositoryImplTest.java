package nl.vpro.domain.api.media;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import nl.vpro.domain.api.topspin.Recommendations;

public class TopSpinRepositoryImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(9999));
    
    @Test
    public void testNoExists() {
        wireMockRule.stubFor(get(urlEqualTo("/notexist.json")).willReturn(notFound()));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/";
        Recommendations forMid = repo.getForMid("notexist", null);
        assertNotNull(forMid);
        assertTrue(forMid.getRecommendations().isEmpty());
    }
    
    @Test
    public void testExists() throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream("/topspin-response.json"), StandardCharsets.UTF_8);
        wireMockRule.stubFor(get(urlEqualTo("/exists.json")).willReturn(okJson(json)));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/";
        Recommendations forMid = repo.getForMid("exists", null);
        assertEquals(87, forMid.getRecommendations().size());
    }

}
