package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.topspin.Recommendations;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@Slf4j
public class TopSpinRepositoryImpl implements TopSpinRepository {


    @Value("${topspin.recommendations.url}")
    protected String topspinUrl;

    @Value("${topspin.recommendations.maxResults}")
    protected String topspinMaxResults;

    @PostConstruct
    public void init() {
        log.info("Connecting with {} for related results (max results: {})", topspinUrl, topspinMaxResults);
    }

    @Override
    public Recommendations getForMid(String mid) {
        WebTarget target = getTopSpinClient().target(topspinUrl + mid + ".json").queryParam("max", topspinMaxResults);
        return target.request().get().readEntity(Recommendations.class);
    }

    protected Client getTopSpinClient() {
        Client client = new ResteasyClientBuilder().build();
        return client;
    }
}
