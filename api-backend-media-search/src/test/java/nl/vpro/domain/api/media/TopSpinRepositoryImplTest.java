package nl.vpro.domain.api.media;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.WireMockServer;

import nl.vpro.domain.api.topspin.Recommendations;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.lanwen.wiremock.ext.WiremockResolver.Wiremock;
import static ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
public class TopSpinRepositoryImplTest {


    @Test
    public void testNoExists(@Wiremock WireMockServer server, @WiremockUri String ur) {
        server.stubFor(get(urlEqualTo("/notexist")).willReturn(notFound()));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/{mediaId}";
        Recommendations forMid = repo.getForMid("notexist", null, null);
        assertNotNull(forMid);
        assertTrue(forMid.getRecommendations().isEmpty());
    }

    @Test
    public void testExists(@Wiremock WireMockServer server, @WiremockUri String url) throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream("/topspin-response.json"), StandardCharsets.UTF_8);
        server.stubFor(get(urlEqualTo("/exists")).willReturn(okJson(json)));
        TopSpinRepositoryImpl repo = new TopSpinRepositoryImpl();
        repo.topspinUrl = "http://localhost:9999/{mediaId}";
        Recommendations forMid = repo.getForMid("exists", null, null);
        assertThat(forMid.getRecommendations().size()).isEqualTo(87);
    }

}
