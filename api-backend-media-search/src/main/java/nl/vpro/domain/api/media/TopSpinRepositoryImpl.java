package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

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

    protected String clazz = "related-series";

    @PostConstruct
    public void init() {
        log.info("Connecting with {} for related results (max results: {})", topspinUrl, topspinMaxResults);
    }

    @Override
    public Recommendations getForMid(String mid, String partyId) {
        String url = topspinUrl + mid + ".json";
        WebTarget target = getTopSpinClient().target(url).queryParam("max", topspinMaxResults);
        Response response = target.request().get();
        if(response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
            log.warn("Could not get top spin recommendations for on url {} (status {})", url, response.getStatus());
            return new Recommendations();
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("mediaId", mid);
            replacements.put("class", clazz);

            WebTarget target = getTopSpinClient().target(topspinUrl).resolveTemplates(replacements)
                .queryParam("max", topspinMaxResults)
                .queryParam("partyId", partyId)
                ;
            Response response = target.request().get();
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                log.warn("Could not get top spin recommendations for on url {} (status {})", topspinUrl, response.getStatus());
                return new Recommendations();
            }
            return response.readEntity(Recommendations.class);
        }
        return response.readEntity(Recommendations.class);
    }

    protected Client getTopSpinClient() {
        return new ResteasyClientBuilder().build();
    }

    @Override
    public String toString() {
        return topspinUrl;
    }
}
