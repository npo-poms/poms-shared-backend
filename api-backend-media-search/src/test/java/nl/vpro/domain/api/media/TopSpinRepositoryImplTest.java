package nl.vpro.domain.api.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import nl.vpro.domain.api.topspin.Recommendations;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
public class TopSpinRepositoryImplTest {


    @Test
    public void testNoExists(WireMockRuntimeInfo wireMockRuntimeInfo) {
        WireMock.stubFor(get(urlEqualTo("/notexist")).willReturn(notFound()));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/{mediaId}";
        Recommendations forMid = repo.getForMid("notexist", null, null);
        assertNotNull(forMid);
        assertTrue(forMid.getRecommendations().isEmpty());
    }

    @Test
    @Disabled
    public void testExists(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream("/topspin-response.json"), StandardCharsets.UTF_8);
        WireMock.stubFor(get(urlEqualTo("/exists/midthatexists")).willReturn(okJson(json)));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/{mediaId}";
        Recommendations forMid = repo.getForMid("midthatexists", null, null);
        assertThat(forMid.getRecommendations().size()).isEqualTo(87);
    }

}
