package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
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
        if (StringUtils.isBlank(topspinUrl)) {
            log.debug("{} is disabled", this);
        } else {
            log.info("Connecting with {} for related results (max results: {})", topspinUrl, topspinMaxResults);
        }
    }

    @Override
    public Recommendations getForMid(String mid) {
        if (StringUtils.isBlank(topspinUrl)) {
            log.warn("Topspin repository is disabled");
            return new Recommendations();
        } else {
            String url = topspinUrl + mid;
            WebTarget target = getTopSpinClient().target(url).queryParam("max", topspinMaxResults);
            Response response = target.request().get();
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                log.warn("Could not get top spin recommendations for on url {} (status {})", url, response.getStatus());
                return new Recommendations();
            }
            return response.readEntity(Recommendations.class);
        }
    }

    protected Client getTopSpinClient() {
        return new ResteasyClientBuilder().build();
    }

    @Override
    public String toString() {
        return topspinUrl;
    }
}
