package nl.vpro.api.service;

import nl.vpro.api.rs.error.NotFoundException;
import nl.vpro.api.rs.error.ServerErrorException;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Date: 24-4-12
 * Time: 13:12
 *
 * @author Ernst Bunders
 */
@Service("ugcService")
public class UgcServiceRestImpl implements UgcService {

    @Autowired
    private UrlProvider ugcUrlprovider;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Annotation getAnnotation(String id) {
        return getById("annotation", id, Annotation.class);
    }

    @Override
    public PlayerConfiguration getPlayerConfiguration(String id) {
        return getById("playerconfiguration", id, PlayerConfiguration.class);
    }





    private <T> T getById(String typeName, String id, Class<T> type) {
        Object[] args = {ugcUrlprovider.getUrl(), id};
        try {
            ResponseEntity<T> responseEntity = restTemplate.getForEntity("{url}" + typeName + "/{urn}.json", type, args);
            return responseEntity.getBody();
        } catch (HttpClientErrorException cee) {
            throw new NotFoundException("Could not fetch annotation with id " + id + ". It does not exist");
        } catch (Exception e) {
            throw new ServerErrorException("Something went wrong fetching an annotation from the ugc service: " + e.getMessage(), e);
        }
    }
}
