package nl.vpro.domain.api.media;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
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
        String url = topspinUrl + mid + ".json";
        WebTarget target = getTopSpinClient().target(url).queryParam("max", topspinMaxResults);
        Response response = target.request().get();
        if(response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
            log.warn("Could not get top spin recommendations for on url {} (status {})", url, response.getStatus());
            return new Recommendations();
        }
        return response.readEntity(Recommendations.class);
    }

    protected Client getTopSpinClient() {
        return new ResteasyClientBuilder().build();
    }
}
