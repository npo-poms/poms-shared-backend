package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.topspin.Recommendations;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@Log4j2
public class TopSpinRepositoryImpl implements TopSpinRepository {


    @Value("${topspin.recommendations.url}")
    protected String topspinUrl;

    @Value("${topspin.recommendations.maxResults}")
    protected String topspinMaxResults;

    protected String clazz = "related-series";

    protected ResteasyClient client;

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(topspinUrl)) {
            log.debug("{} is disabled", this);
        } else {
            log.info("Connecting with {} for related results (max results: {})", topspinUrl, topspinMaxResults);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public Recommendations getForMid(String mid, String partyId, String clazz) {
        if (StringUtils.isBlank(topspinUrl)) {
            log.warn("Topspin repository is disabled");
            return new Recommendations();
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("mediaId", mid);
            replacements.put("class", clazz == null ? this.clazz : clazz);

            WebTarget target = getTopSpinClient().target(topspinUrl).resolveTemplates(replacements)
                .queryParam("max", topspinMaxResults)
                .queryParam("partyId", partyId)
                ;
            try (Response response = target.request().get()) {
                if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                    log.warn("Could not get top spin recommendations for on url {} (status {})", topspinUrl, response.getStatus());
                    return new Recommendations();
                }
                return response.readEntity(Recommendations.class);
            }
        }
    }

    protected synchronized  Client getTopSpinClient() {
        if (client == null) {
            client = new ResteasyClientBuilderImpl().build();
        }
        return client;
    }

    @Override
    public String toString() {
        return topspinUrl;
    }
}
